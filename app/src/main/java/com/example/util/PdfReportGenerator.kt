package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.data.model.AppSetting
import com.example.data.model.Customer
import com.example.data.model.CustomerMonthlyBreakdown
import com.example.data.model.Transaction
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfReportGenerator {

    private const val PAGE_WIDTH = 595 // A4 standard width in points
    private const val PAGE_HEIGHT = 842 // A4 standard height in points
    private const val MARGIN = 30f

    /**
     * Generates a detailed Account Ledger Statement for a single customer.
     */
    fun generateCustomerLedgerPdf(
        context: Context,
        shopSettings: Map<String, String>,
        customer: Customer,
        transactions: List<Transaction>,
        monthFilter: String? = null
    ): File? {
        val document = PdfDocument()
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        val filteredTransactions = if (monthFilter != null) {
            transactions.filter { it.entryDate.startsWith(monthFilter) }
        } else {
            transactions
        }.sortedWith(compareBy({ it.entryDate }, { it.entryTime }, { it.id }))

        var currentPageNum = 1
        var pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, currentPageNum).create()
        var page = document.startPage(pageInfo)
        var canvas = page.canvas

        // Header
        var y = drawHeader(canvas, paint, shopSettings, "ग्राहक खाता विवरण")

        // Customer Info Card
        y = drawCustomerInfo(canvas, paint, customer, y, monthFilter)

        // Table Header
        y = drawCustomerTableHeader(canvas, paint, y)

        var runningBalance = 0L
        var totalUdhar = 0L
        var totalJama = 0L

        // Prior balance if month filter is used
        if (monthFilter != null) {
            val priorTransactions = transactions.filter { it.entryDate < monthFilter }
            val priorUdhar = priorTransactions.filter { it.isCredit }.sumOf { it.amountPaise }
            val priorJama = priorTransactions.filter { it.isPayment }.sumOf { it.amountPaise }
            val priorBalance = priorUdhar - priorJama
            runningBalance = priorBalance

            if (priorBalance != 0L) {
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
                paint.textSize = 10f
                paint.color = Color.DKGRAY
                canvas.drawText("Opening Balance: ${CurrencyUtils.formatPaiseToRupees(priorBalance)}", MARGIN + 6, y + 14, paint)
                y += 24
            }
        }

        // Rows
        for (txn in filteredTransactions) {
            if (y > PAGE_HEIGHT - 80) {
                drawPageFooter(canvas, paint, currentPageNum)
                document.finishPage(page)

                currentPageNum++
                pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, currentPageNum).create()
                page = document.startPage(pageInfo)
                canvas = page.canvas
                y = drawHeader(canvas, paint, shopSettings, "ग्राहक खाता विवरण (क्रमशः)")
                y = drawCustomerTableHeader(canvas, paint, y)
            }

            if (txn.isCredit) {
                totalUdhar += txn.amountPaise
                runningBalance += txn.amountPaise
            } else {
                totalJama += txn.amountPaise
                runningBalance -= txn.amountPaise
            }

            y = drawCustomerRow(canvas, paint, txn, runningBalance, y)
        }

        // Summary footer box
        if (y > PAGE_HEIGHT - 120) {
            drawPageFooter(canvas, paint, currentPageNum)
            document.finishPage(page)

            currentPageNum++
            pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, currentPageNum).create()
            page = document.startPage(pageInfo)
            canvas = page.canvas
            y = drawHeader(canvas, paint, shopSettings, "ग्राहक खाता विवरण (सारांश)")
        }

        drawCustomerSummaryBox(canvas, paint, totalUdhar, totalJama, runningBalance, y)
        drawPageFooter(canvas, paint, currentPageNum)
        document.finishPage(page)

        val fileName = "Khata_${customer.customerCode}_${System.currentTimeMillis()}.pdf"
        return savePdfToFile(context, document, fileName)
    }

    /**
     * Generates a monthly customer summary report in 8-column table format sorted by customer ID.
     * Columns: [क्र., कोड, ग्राहक का नाम, मासिक उधार, पिछला बकाया, उधार देय, मासिक जमा, लेन-देन]
     * Includes a 5-6 line Hindi note at the end explaining the keywords.
     */
    fun generateMonthlyTransactionsPdf(
        context: Context,
        shopSettings: Map<String, String>,
        monthYear: String,
        breakdowns: List<CustomerMonthlyBreakdown>
    ): File? {
        val document = PdfDocument()
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Sort customer list by customer ID as requested
        val sortedBreakdowns = breakdowns.sortedBy { it.customer.id }

        var currentPageNum = 1
        var pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, currentPageNum).create()
        var page = document.startPage(pageInfo)
        var canvas = page.canvas

        val monthName = DateTimeUtils.formatMonthToHindi(monthYear)
        var y = drawHeader(canvas, paint, shopSettings, "मासिक ग्राहक रिपोर्ट सारणी ($monthName)")

        // Table Header
        y = drawMonthlyTableReportHeader(canvas, paint, y)

        var totalMonthUdhar = 0L
        var totalPichlaBakaya = 0L
        var totalPrabhaviUdhar = 0L
        var totalMonthJama = 0L
        var totalMonthEntries = 0

        if (sortedBreakdowns.isEmpty()) {
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
            paint.textSize = 10f
            paint.color = Color.GRAY
            canvas.drawText("इस माह में कोई ग्राहक विवरण उपलब्ध नहीं है।", MARGIN + 12, y + 20, paint)
            y += 40f
        } else {
            for ((index, item) in sortedBreakdowns.withIndex()) {
                if (y > PAGE_HEIGHT - 70) {
                    drawPageFooter(canvas, paint, currentPageNum)
                    document.finishPage(page)

                    currentPageNum++
                    pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, currentPageNum).create()
                    page = document.startPage(pageInfo)
                    canvas = page.canvas
                    y = drawHeader(canvas, paint, shopSettings, "मासिक ग्राहक रिपोर्ट सारणी ($monthName - क्रमशः)")
                    y = drawMonthlyTableReportHeader(canvas, paint, y)
                }

                totalMonthUdhar += item.monthlyCreditPaise
                totalPichlaBakaya += item.pichlaBakayaPaise
                totalPrabhaviUdhar += item.prabhaviUdharPaise
                totalMonthJama += item.monthlyPaymentPaise
                totalMonthEntries += item.transactionCountInMonth

                y = drawMonthlyTableReportRow(canvas, paint, index + 1, item, y)
            }
        }

        // Check if totals box and note fit on current page; if not, create new page
        val requiredBottomSpace = 160f
        if (y > PAGE_HEIGHT - requiredBottomSpace) {
            drawPageFooter(canvas, paint, currentPageNum)
            document.finishPage(page)

            currentPageNum++
            pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, currentPageNum).create()
            page = document.startPage(pageInfo)
            canvas = page.canvas
            y = drawHeader(canvas, paint, shopSettings, "मासिक रिपोर्ट सारांश एवं विवरण")
        }

        // Totals Box
        y = drawMonthlyTableReportTotals(
            canvas = canvas,
            paint = paint,
            totalCustomers = sortedBreakdowns.size,
            totalUdhar = totalMonthUdhar,
            totalPichlaBakaya = totalPichlaBakaya,
            totalPrabhaviUdhar = totalPrabhaviUdhar,
            totalJama = totalMonthJama,
            totalEntries = totalMonthEntries,
            startY = y
        )

        // Hindi Terminology Note (5-6 lines)
        y = drawMonthlyReportTerminologyNote(canvas, paint, y)

        drawPageFooter(canvas, paint, currentPageNum)
        document.finishPage(page)

        val fileName = "Monthly_Table_Report_${monthYear}_${System.currentTimeMillis()}.pdf"
        return savePdfToFile(context, document, fileName)
    }

    /**
     * Generates a 3-Column Table Grid Bill Voucher ([PART-B] Monthly Bill) matching the exact print slip format.
     * Only generates bills for customers who have non-zero 'उधार देय'.
     * Includes 'Pichla Bakaya' and 'उधार देय' rows when Pichla Bakaya is non-zero.
     */
    fun generateMonthlyCustomerVouchersPdf(
        context: Context,
        shopSettings: Map<String, String>,
        monthYear: String,
        breakdowns: List<CustomerMonthlyBreakdown>
    ): File? {
        val document = PdfDocument()
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Only make bill of customers who have non-zero 'उधार देय' (prabhaviUdharPaise != 0)
        val nonZeroUdharDeyBreakdowns = breakdowns.filter { it.prabhaviUdharPaise != 0L }
        val sortedBreakdowns = nonZeroUdharDeyBreakdowns.sortedBy { it.customer.id }
        val monthEnglish = DateTimeUtils.formatMonthToEnglish(monthYear)
        val itemsPerPage = 24 // 8 rows x 3 columns for spacious 88f cell height
        val totalItems = sortedBreakdowns.size
        val totalPages = if (totalItems == 0) 1 else (totalItems + itemsPerPage - 1) / itemsPerPage

        val gridLeft = 20f
        val gridRight = PAGE_WIDTH - 20f
        val gridWidth = gridRight - gridLeft
        val colWidth = gridWidth / 3f
        val cellHeight = 88f
        val gridTop = 50f

        for (pageNum in 1..totalPages) {
            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNum).create()
            val page = document.startPage(pageInfo)
            val canvas = page.canvas

            // 1. Draw Title Header at Top (Centered Teal)
            paint.textAlign = Paint.Align.CENTER
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 15f
            paint.color = Color.rgb(14, 116, 144) // Teal / Slate Cyan
            val titleText = if (totalPages > 1) {
                "[PART-B] Monthly Bill - $monthEnglish (Page $pageNum)"
            } else {
                "[PART-B] Monthly Bill - $monthEnglish"
            }
            canvas.drawText(titleText, PAGE_WIDTH / 2f, 36f, paint)

            if (totalItems == 0) {
                paint.textAlign = Paint.Align.CENTER
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                paint.textSize = 11f
                paint.color = Color.rgb(100, 116, 139)
                canvas.drawText("इस माह में किसी भी ग्राहक का उधार देय शेष नहीं है।", PAGE_WIDTH / 2f, 150f, paint)
            } else {
                // 2. Draw 3-Column Vouchers Grid
                val startIndex = (pageNum - 1) * itemsPerPage
                val endIndex = kotlin.math.min(startIndex + itemsPerPage, totalItems)

                for (i in startIndex until endIndex) {
                    val item = sortedBreakdowns[i]
                    val localIndex = i - startIndex
                    val row = localIndex / 3
                    val col = localIndex % 3

                    val cellX = gridLeft + col * colWidth
                    val cellY = gridTop + row * cellHeight

                    drawVoucherCell(
                        canvas = canvas,
                        paint = paint,
                        item = item,
                        monthEnglish = monthEnglish,
                        x = cellX,
                        y = cellY,
                        width = colWidth,
                        height = cellHeight
                    )
                }

                // Draw empty grid cells for incomplete rows to maintain a clean rectangular table
                val remainder = (endIndex - startIndex) % 3
                if (remainder != 0) {
                    val lastRow = (endIndex - startIndex) / 3
                    for (col in remainder until 3) {
                        val emptyCellX = gridLeft + col * colWidth
                        val emptyCellY = gridTop + lastRow * cellHeight
                        drawEmptyGridCell(canvas, paint, emptyCellX, emptyCellY, colWidth, cellHeight)
                    }
                }
            }

            // 3. Draw Page Footer
            paint.textAlign = Paint.Align.LEFT
            drawPageFooter(canvas, paint, pageNum)
            document.finishPage(page)
        }

        val fileName = "Monthly_Bill_Vouchers_${monthYear}_${System.currentTimeMillis()}.pdf"
        return savePdfToFile(context, document, fileName)
    }

    private fun drawVoucherCell(
        canvas: Canvas,
        paint: Paint,
        item: CustomerMonthlyBreakdown,
        monthEnglish: String,
        x: Float,
        y: Float,
        width: Float,
        height: Float
    ) {
        // Border outline
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 0.8f
        paint.color = Color.rgb(148, 163, 184)
        canvas.drawRect(x, y, x + width, y + height, paint)

        // Text setup
        paint.style = Paint.Style.FILL
        paint.textAlign = Paint.Align.LEFT
        val padX = x + 6f
        var textY = y + 12f

        // Line 1: महीना: August 2026 का हिसाब :-
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 8.5f
        paint.color = Color.rgb(30, 41, 59)
        canvas.drawText("महीना: $monthEnglish का हिसाब :-", padX, textY, paint)

        // Line 2: Customer Name
        textY += 12f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 9.5f
        paint.color = Color.rgb(15, 23, 42)

        val fullName = item.customer.name
        val maxTextWidth = width - 12f
        val displayName = if (paint.measureText(fullName) > maxTextWidth) {
            var truncated = fullName
            while (truncated.length > 3 && paint.measureText("$truncated..") > maxTextWidth) {
                truncated = truncated.dropLast(1)
            }
            "$truncated.."
        } else {
            fullName
        }
        canvas.drawText(displayName, padX, textY, paint)

        // Line 3: मासिक उधार: ₹ 2855
        textY += 12f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 8.5f
        paint.color = Color.rgb(220, 38, 38)
        val hasDecimal = (item.monthlyCreditPaise % 100 != 0L)
        val formattedAmount = CurrencyUtils.formatPaiseToRupees(
            item.monthlyCreditPaise,
            includeSymbol = false,
            includeDecimal = hasDecimal
        )
        canvas.drawText("मासिक उधार: ₹ $formattedAmount", padX, textY, paint)

        // If Pichla Bakaya is Non-Zero, write two more rows: 'Pichla Bakaya' and 'Prabhavi Udhaar'
        if (item.pichlaBakayaPaise != 0L) {
            textY += 11f
            paint.textSize = 8f
            val pichlaSign = if (item.pichlaBakayaPaise > 0) "+ " else "- "
            val pichlaAmt = CurrencyUtils.formatPaiseToRupees(
                kotlin.math.abs(item.pichlaBakayaPaise),
                includeSymbol = false,
                includeDecimal = item.pichlaBakayaPaise % 100 != 0L
            )
            paint.color = if (item.pichlaBakayaPaise > 0) Color.rgb(220, 38, 38) else Color.rgb(22, 163, 74)
            canvas.drawText("पिछला बकाया: $pichlaSign₹ $pichlaAmt", padX, textY, paint)

            textY += 11f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 8.5f
            val prabhaviAmt = CurrencyUtils.formatPaiseToRupees(
                item.prabhaviUdharPaise,
                includeSymbol = false,
                includeDecimal = item.prabhaviUdharPaise % 100 != 0L
            )
            paint.color = if (item.prabhaviUdharPaise > 0) Color.rgb(185, 28, 28) else Color.rgb(21, 128, 61)
            canvas.drawText("उधार देय: ₹ $prabhaviAmt", padX, textY, paint)

            // Line: हस्त:
            textY += 11f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.textSize = 7.5f
            paint.color = Color.rgb(30, 41, 59)
            canvas.drawText("हस्त:", padX, textY, paint)

            // Line: Dotted Line for Signature
            textY += 9f
            paint.textSize = 7.5f
            paint.color = Color.rgb(100, 116, 139)
            canvas.drawText("....................................................", padX, textY, paint)
        } else {
            // Line 4: हस्त:
            textY += 14f
            paint.textSize = 8f
            paint.color = Color.rgb(30, 41, 59)
            canvas.drawText("हस्त:", padX, textY, paint)

            // Line 5: Dotted Line for Signature
            textY += 12f
            paint.textSize = 8f
            paint.color = Color.rgb(100, 116, 139)
            canvas.drawText("....................................................", padX, textY, paint)
        }
    }

    private fun drawEmptyGridCell(
        canvas: Canvas,
        paint: Paint,
        x: Float,
        y: Float,
        width: Float,
        height: Float
    ) {
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 0.8f
        paint.color = Color.rgb(148, 163, 184)
        canvas.drawRect(x, y, x + width, y + height, paint)
    }

    // --- Canvas Helpers ---

    private fun drawHeader(
        canvas: Canvas,
        paint: Paint,
        shopSettings: Map<String, String>,
        subTitle: String
    ): Float {
        val shopName = shopSettings[AppSetting.KEY_SHOP_NAME]?.ifBlank { "डिजिटल बही-खाता" } ?: "डिजिटल बही-खाता"
        val ownerName = shopSettings[AppSetting.KEY_OWNER_NAME]?.ifBlank { "" } ?: ""
        val address = shopSettings[AppSetting.KEY_ADDRESS]?.ifBlank { "" } ?: ""
        val mobile = shopSettings[AppSetting.KEY_MOBILE]?.ifBlank { "" } ?: ""

        // Top decorative bar (Maroon)
        paint.color = Color.rgb(153, 27, 27)
        canvas.drawRect(MARGIN, MARGIN, PAGE_WIDTH - MARGIN, MARGIN + 4, paint)

        var y = MARGIN + 22f

        // Shop Name
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 18f
        paint.color = Color.rgb(153, 27, 27)
        canvas.drawText(shopName, MARGIN, y, paint)

        // Subtitle
        y += 18f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 12f
        paint.color = Color.rgb(30, 41, 59)
        canvas.drawText(subTitle, MARGIN, y, paint)

        // Shop metadata
        var details = mutableListOf<String>()
        // if (ownerName.isNotBlank()) details.add("मालिक: $ownerName")
        if (mobile.isNotBlank()) details.add("मो: $mobile")
        if (address.isNotBlank()) details.add("पता: $address")

        val detailStr = details.joinToString(" | ")
        if (detailStr.isNotBlank()) {
            y += 14f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.textSize = 9.5f
            paint.color = Color.DKGRAY
            canvas.drawText(detailStr, MARGIN, y, paint)
        }

        // Date generated
        val dateGen = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.US).format(Date())
        paint.textSize = 8.5f
        paint.color = Color.GRAY
        val dateGenText = "प्रिंट दिनांक: $dateGen"
        val textWidth = paint.measureText(dateGenText)
        canvas.drawText(dateGenText, PAGE_WIDTH - MARGIN - textWidth, MARGIN + 22f, paint)

        // Separator line
        y += 12f
        paint.color = Color.LTGRAY
        paint.strokeWidth = 1f
        canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, paint)

        return y + 10f
    }

    private fun drawCustomerInfo(
        canvas: Canvas,
        paint: Paint,
        customer: Customer,
        startY: Float,
        monthFilter: String?
    ): Float {
        val rect = RectF(MARGIN, startY, PAGE_WIDTH - MARGIN, startY + 45f)
        paint.color = Color.rgb(248, 249, 250)
        canvas.drawRoundRect(rect, 4f, 4f, paint)
        paint.style = Paint.Style.STROKE
        paint.color = Color.rgb(226, 232, 240)
        paint.strokeWidth = 1f
        canvas.drawRoundRect(rect, 4f, 4f, paint)
        paint.style = Paint.Style.FILL

        var y = startY + 16f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 14f
        paint.color = Color.rgb(15, 23, 42)
        canvas.drawText("ग्राहक: ${customer.name}", MARGIN + 10, y, paint)

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 9f
        paint.color = Color.rgb(71, 85, 105)
        canvas.drawText("कोड: ${customer.customerCode}", PAGE_WIDTH - MARGIN - 130, y, paint)

        y += 18f
        val mobileText = if (!customer.mobileNumber.isNullOrBlank()) "मो: ${customer.mobileNumber}" else ""
        val addressText = if (!customer.address.isNullOrBlank()) "पता: ${customer.address}" else ""
        val infoLine = listOf(mobileText, addressText).filter { it.isNotBlank() }.joinToString("  •  ")
        if (infoLine.isNotBlank()) {
            canvas.drawText(infoLine, MARGIN + 10, y, paint)
        }

        if (monthFilter != null) {
            val mText = "अवधि: ${DateTimeUtils.formatMonthToHindi(monthFilter)}"
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText(mText, PAGE_WIDTH - MARGIN - 130, y, paint)
        }

        return startY + 54f
    }

    private fun drawCustomerTableHeader(canvas: Canvas, paint: Paint, startY: Float): Float {
        val h = 22f
        paint.color = Color.rgb(238, 242, 246)
        canvas.drawRect(MARGIN, startY, PAGE_WIDTH - MARGIN, startY + h, paint)

        paint.color = Color.rgb(15, 23, 42)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 9.5f

        val y = startY + 15f
        canvas.drawText("दिनांक/समय", MARGIN + 6, y, paint)
        canvas.drawText("विवरण", MARGIN + 95, y, paint)
        canvas.drawText("उधार (-)", MARGIN + 280, y, paint)
        canvas.drawText("जमा (+)", MARGIN + 365, y, paint)
        canvas.drawText("शेष (Balance)", MARGIN + 445, y, paint)

        return startY + h + 4f
    }

    private fun drawCustomerRow(
        canvas: Canvas,
        paint: Paint,
        txn: Transaction,
        runningBalance: Long,
        startY: Float
    ): Float {
        val rowHeight = 22f
        val y = startY + 14f

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 9f
        paint.color = Color.rgb(51, 65, 85)

        val dateDisplay = "${DateTimeUtils.formatToShortHindiDate(txn.entryDate)} ${DateTimeUtils.formatTo12HourTime(txn.entryTime)}"
        canvas.drawText(dateDisplay, MARGIN + 6, y, paint)

        val desc = txn.description?.ifBlank { "-" } ?: "-"
        val cleanDesc = if (desc.length > 28) desc.take(25) + "..." else desc
        canvas.drawText(cleanDesc, MARGIN + 95, y, paint)

        if (txn.isCredit) {
            paint.color = Color.rgb(220, 38, 38) // Udhar Red
            canvas.drawText(CurrencyUtils.formatPaiseToRupees(txn.amountPaise), MARGIN + 280, y, paint)
            paint.color = Color.LTGRAY
            canvas.drawText("-", MARGIN + 375, y, paint)
        } else {
            paint.color = Color.LTGRAY
            canvas.drawText("-", MARGIN + 290, y, paint)
            paint.color = Color.rgb(22, 163, 74) // Jama Green
            canvas.drawText(CurrencyUtils.formatPaiseToRupees(txn.amountPaise), MARGIN + 365, y, paint)
        }

        // Running balance
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.color = if (runningBalance > 0) Color.rgb(220, 38, 38) else Color.rgb(22, 163, 74)
        val balText = CurrencyUtils.formatPaiseToRupees(runningBalance)
        canvas.drawText(balText, MARGIN + 445, y, paint)

        // Dotted bottom line
        paint.color = Color.rgb(241, 245, 249)
        paint.strokeWidth = 0.8f
        canvas.drawLine(MARGIN, startY + rowHeight, PAGE_WIDTH - MARGIN, startY + rowHeight, paint)

        return startY + rowHeight
    }

    private fun drawCustomerSummaryBox(
        canvas: Canvas,
        paint: Paint,
        totalUdhar: Long,
        totalJama: Long,
        finalBalance: Long,
        startY: Float
    ) {
        val rect = RectF(MARGIN, startY + 6, PAGE_WIDTH - MARGIN, startY + 54f)
        paint.color = Color.rgb(254, 243, 199)
        canvas.drawRoundRect(rect, 4f, 4f, paint)

        val y = startY + 24f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 10f
        paint.color = Color.rgb(180, 83, 9)

        canvas.drawText("कुल उधार: ${CurrencyUtils.formatPaiseToRupees(totalUdhar)}", MARGIN + 12, y, paint)
        canvas.drawText("कुल जमा: ${CurrencyUtils.formatPaiseToRupees(totalJama)}", MARGIN + 175, y, paint)

        paint.textSize = 11f
        paint.color = if (finalBalance > 0) Color.rgb(185, 28, 28) else Color.rgb(21, 128, 61)
        val status = if (finalBalance > 0) "कुल बाकी (उधार शेष)" else if (finalBalance < 0) "जमा शेष (अग्रिम)" else "बराबर (Nil)"
        canvas.drawText("$status: ${CurrencyUtils.formatPaiseToRupees(finalBalance)}", MARGIN + 335, y, paint)

        // Signature text
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 8.5f
        paint.color = Color.DKGRAY
        canvas.drawText("हस्ताक्षर (दुकानदार)", PAGE_WIDTH - MARGIN - 110, startY + 44f, paint)
    }

    private fun drawMonthlyTableReportHeader(canvas: Canvas, paint: Paint, startY: Float): Float {
        val h = 22f
        paint.style = Paint.Style.FILL
        paint.color = Color.rgb(238, 242, 246)
        canvas.drawRect(MARGIN, startY, PAGE_WIDTH - MARGIN, startY + h, paint)

        val y = startY + 15f

        // 1. क्र.
        paint.textAlign = Paint.Align.LEFT
        paint.color = Color.rgb(15, 23, 42)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 8.5f
        canvas.drawText("क्र.", MARGIN + 4f, y, paint)

        // 2. कोड (Compact, small light grey)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 7f
        paint.color = Color.rgb(100, 116, 139)
        canvas.drawText("कोड", MARGIN + 24f, y, paint)

        // 3. ग्राहक का नाम (Full space, bold black)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 8.5f
        paint.color = Color.rgb(15, 23, 42)
        canvas.drawText("ग्राहक का नाम", MARGIN + 66f, y, paint)

        // 4. मासिक उधार
        paint.textAlign = Paint.Align.RIGHT
        paint.color = Color.rgb(220, 38, 38)
        paint.textSize = 8.5f
        canvas.drawText("मासिक उधार", MARGIN + 268f, y, paint)

        // 5. पिछला बकाया
        paint.color = Color.rgb(71, 85, 105)
        paint.textSize = 8f
        canvas.drawText("पिछला बकाया", MARGIN + 342f, y, paint)

        // 6. उधार देय
        paint.color = Color.rgb(185, 28, 28)
        paint.textSize = 8.5f
        canvas.drawText("उधार देय", MARGIN + 418f, y, paint)

        // 7. मासिक जमा
        paint.color = Color.rgb(22, 163, 74)
        paint.textSize = 8.5f
        canvas.drawText("मासिक जमा", MARGIN + 482f, y, paint)

        // 8. लेन-देन
        paint.textAlign = Paint.Align.CENTER
        paint.color = Color.rgb(71, 85, 105)
        paint.textSize = 8f
        canvas.drawText("लेन-देन", MARGIN + 524f, y, paint)

        paint.textAlign = Paint.Align.LEFT
        return startY + h + 4f
    }

    private fun drawMonthlyTableReportRow(
        canvas: Canvas,
        paint: Paint,
        rowNum: Int,
        item: CustomerMonthlyBreakdown,
        startY: Float
    ): Float {
        val rowHeight = 20f
        val y = startY + 13f

        paint.style = Paint.Style.FILL

        // 1. क्र. (Row Number)
        paint.textAlign = Paint.Align.LEFT
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 8.5f
        paint.color = Color.rgb(51, 65, 85)
        canvas.drawText("$rowNum", MARGIN + 4f, y, paint)

        // 2. कोड (Customer Code in very small size font, compact column width, light grey)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 6.8f
        paint.color = Color.rgb(100, 116, 139)
        canvas.drawText(item.customer.customerCode, MARGIN + 24f, y, paint)

        // 3. ग्राहक का नाम (Black and bold font, full space so name not trimmed)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 8.5f
        paint.color = Color.rgb(15, 23, 42)
        val maxNameWidth = 135f
        val fullName = item.customer.name
        val displayName = if (paint.measureText(fullName) > maxNameWidth) {
            var truncated = fullName
            while (truncated.length > 3 && paint.measureText("$truncated..") > maxNameWidth) {
                truncated = truncated.dropLast(1)
            }
            "$truncated.."
        } else {
            fullName
        }
        canvas.drawText(displayName, MARGIN + 66f, y, paint)

        // 4. मासिक उधार (Red font, right-aligned)
        paint.textAlign = Paint.Align.RIGHT
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 8.5f
        paint.color = Color.rgb(220, 38, 38)
        val udharText = CurrencyUtils.formatPaiseToRupees(item.monthlyCreditPaise)
        canvas.drawText(udharText, MARGIN + 268f, y, paint)

        // 5. पिछला बकाया (Red & '+' if positive, Green & '-' if negative, Grey if zero, right-aligned)
        paint.textSize = 8f
        val (pichlaText, pichlaColor) = when {
            item.pichlaBakayaPaise > 0 -> "+ ${CurrencyUtils.formatPaiseToRupees(item.pichlaBakayaPaise)}" to Color.rgb(220, 38, 38)
            item.pichlaBakayaPaise < 0 -> "- ${CurrencyUtils.formatPaiseToRupees(kotlin.math.abs(item.pichlaBakayaPaise))}" to Color.rgb(22, 163, 74)
            else -> "₹0" to Color.rgb(148, 163, 184)
        }
        paint.color = pichlaColor
        canvas.drawText(pichlaText, MARGIN + 342f, y, paint)

        // 6. उधार देय (Red & Bold if positive, Green & Bold if negative, right-aligned)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 8.5f
        val (prabhaviText, prabhaviColor) = when {
            item.prabhaviUdharPaise > 0 -> CurrencyUtils.formatPaiseToRupees(item.prabhaviUdharPaise) to Color.rgb(185, 28, 28)
            item.prabhaviUdharPaise < 0 -> CurrencyUtils.formatPaiseToRupees(item.prabhaviUdharPaise) to Color.rgb(21, 128, 61)
            else -> "₹0" to Color.rgb(148, 163, 184)
        }
        paint.color = prabhaviColor
        canvas.drawText(prabhaviText, MARGIN + 418f, y, paint)

        // 7. मासिक जमा (Green font, right-aligned)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 8.5f
        paint.color = Color.rgb(22, 163, 74)
        val jamaText = CurrencyUtils.formatPaiseToRupees(item.monthlyPaymentPaise)
        canvas.drawText(jamaText, MARGIN + 482f, y, paint)

        // 8. लेन-देन (Center-aligned, compact width)
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = 8f
        paint.color = Color.rgb(71, 85, 105)
        canvas.drawText("${item.transactionCountInMonth}", MARGIN + 524f, y, paint)

        // Reset textAlign
        paint.textAlign = Paint.Align.LEFT

        // Row bottom separator line
        paint.color = Color.rgb(241, 245, 249)
        paint.strokeWidth = 0.8f
        canvas.drawLine(MARGIN, startY + rowHeight, PAGE_WIDTH - MARGIN, startY + rowHeight, paint)

        return startY + rowHeight
    }

    private fun drawMonthlyTableReportTotals(
        canvas: Canvas,
        paint: Paint,
        totalCustomers: Int,
        totalUdhar: Long,
        totalPichlaBakaya: Long,
        totalPrabhaviUdhar: Long,
        totalJama: Long,
        totalEntries: Int,
        startY: Float
    ): Float {
        val h = 42f
        val rect = RectF(MARGIN, startY + 4f, PAGE_WIDTH - MARGIN, startY + 4f + h)
        paint.style = Paint.Style.FILL
        paint.color = Color.rgb(254, 243, 199) // Light amber
        canvas.drawRoundRect(rect, 4f, 4f, paint)

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 0.8f
        paint.color = Color.rgb(252, 211, 77)
        canvas.drawRoundRect(rect, 4f, 4f, paint)

        paint.style = Paint.Style.FILL
        val y1 = startY + 19f
        val y2 = startY + 36f

        // Row 1
        paint.textAlign = Paint.Align.LEFT
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 9f
        paint.color = Color.rgb(180, 83, 9)
        canvas.drawText("कुल ग्राहक: $totalCustomers", MARGIN + 8f, y1, paint)

        paint.color = Color.rgb(71, 85, 105)
        canvas.drawText("कुल लेन-देन: $totalEntries", MARGIN + 110f, y1, paint)

        paint.color = Color.rgb(22, 163, 74)
        canvas.drawText("कुल जमा: ${CurrencyUtils.formatPaiseToRupees(totalJama)}", MARGIN + 230f, y1, paint)

        // Row 2
        paint.color = Color.rgb(220, 38, 38)
        canvas.drawText("मासिक उधार: ${CurrencyUtils.formatPaiseToRupees(totalUdhar)}", MARGIN + 8f, y2, paint)

        val pichlaSign = if (totalPichlaBakaya > 0) "+ " else if (totalPichlaBakaya < 0) "- " else ""
        paint.color = if (totalPichlaBakaya >= 0) Color.rgb(220, 38, 38) else Color.rgb(22, 163, 74)
        canvas.drawText("पिछला बकाया: $pichlaSign${CurrencyUtils.formatPaiseToRupees(kotlin.math.abs(totalPichlaBakaya))}", MARGIN + 180f, y2, paint)

        paint.textSize = 9.5f
        paint.color = if (totalPrabhaviUdhar >= 0) Color.rgb(185, 28, 28) else Color.rgb(21, 128, 61)
        canvas.drawText("कुल उधार देय: ${CurrencyUtils.formatPaiseToRupees(totalPrabhaviUdhar)}", MARGIN + 370f, y2, paint)

        return startY + h + 6f
    }

    private fun drawMonthlyReportTerminologyNote(canvas: Canvas, paint: Paint, startY: Float): Float {
        val noteHeight = 84f
        val rect = RectF(MARGIN, startY + 6f, PAGE_WIDTH - MARGIN, startY + 6f + noteHeight)

        paint.style = Paint.Style.FILL
        paint.color = Color.rgb(248, 250, 252) // Slate 50
        canvas.drawRoundRect(rect, 4f, 4f, paint)

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 0.8f
        paint.color = Color.rgb(226, 232, 240) // Slate 200
        canvas.drawRoundRect(rect, 4f, 4f, paint)

        paint.style = Paint.Style.FILL
        paint.textAlign = Paint.Align.LEFT

        var y = startY + 20f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 8.5f
        paint.color = Color.rgb(71, 85, 105) // Slate 600
        canvas.drawText("नोट एवं शब्दावली विवरण (Note & Terminology):", MARGIN + 8f, y, paint)

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 7.5f
        paint.color = Color.rgb(100, 116, 139) // Slate 500

        val lines = listOf(
            "• मासिक उधार: चयनित माह के दौरान ग्राहक द्वारा लिया गया कुल नया उधार।",
            "• पिछला बकाया: चयनित माह से पूर्व का शेष हिसाब (माह की जमा राशि समायोजित करने के बाद)।",
            "• उधार देय: चालू माह तक का कुल देय उधार (मासिक उधार + पिछला बकाया)।",
            "• मासिक जमा: चयनित माह के दौरान ग्राहक द्वारा किया गया कुल नकद भुगतान / जमा राशि।",
            "• लेन-देन: चयनित माह में बही-खाता में दर्ज कुल प्रविष्टियों (Entries) की संख्या।"
        )

        for (line in lines) {
            y += 12f
            canvas.drawText(line, MARGIN + 8f, y, paint)
        }

        return startY + noteHeight + 10f
    }

    private fun drawVouchersTableHeader(canvas: Canvas, paint: Paint, startY: Float): Float {
        val h = 22f
        paint.color = Color.rgb(238, 242, 246)
        canvas.drawRect(MARGIN, startY, PAGE_WIDTH - MARGIN, startY + h, paint)

        paint.color = Color.rgb(15, 23, 42)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 9.5f

        val y = startY + 15f
        canvas.drawText("कोड", MARGIN + 6, y, paint)
        canvas.drawText("ग्राहक का नाम", MARGIN + 65, y, paint)
        canvas.drawText("माह उधार", MARGIN + 210, y, paint)
        canvas.drawText("माह जमा", MARGIN + 295, y, paint)
        canvas.drawText("माह का अंतर", MARGIN + 380, y, paint)
        canvas.drawText("वर्तमान बकाया", MARGIN + 460, y, paint)

        return startY + h + 4f
    }

    private fun drawVoucherRow(
        canvas: Canvas,
        paint: Paint,
        item: CustomerMonthlyBreakdown,
        startY: Float
    ): Float {
        val rowHeight = 20f
        val y = startY + 13f

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 8.5f
        paint.color = Color.rgb(51, 65, 85)

        canvas.drawText(item.customer.customerCode, MARGIN + 6, y, paint)

        val cName = if (item.customer.name.length > 20) item.customer.name.take(18) + ".." else item.customer.name
        canvas.drawText(cName, MARGIN + 65, y, paint)

        paint.color = Color.rgb(220, 38, 38)
        canvas.drawText(CurrencyUtils.formatPaiseToRupees(item.monthlyCreditPaise), MARGIN + 210, y, paint)

        paint.color = Color.rgb(22, 163, 74)
        canvas.drawText(CurrencyUtils.formatPaiseToRupees(item.monthlyPaymentPaise), MARGIN + 295, y, paint)

        paint.color = if (item.netMonthlyBalancePaise > 0) Color.rgb(220, 38, 38) else Color.rgb(22, 163, 74)
        canvas.drawText(CurrencyUtils.formatPaiseToRupees(item.netMonthlyBalancePaise), MARGIN + 380, y, paint)

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.color = if (item.currentTotalBakayaPaise > 0) Color.rgb(185, 28, 28) else Color.rgb(21, 128, 61)
        canvas.drawText(CurrencyUtils.formatPaiseToRupees(item.currentTotalBakayaPaise), MARGIN + 460, y, paint)

        paint.color = Color.rgb(241, 245, 249)
        canvas.drawLine(MARGIN, startY + rowHeight, PAGE_WIDTH - MARGIN, startY + rowHeight, paint)

        return startY + rowHeight
    }

    private fun drawVoucherGrandTotals(
        canvas: Canvas,
        paint: Paint,
        grandUdhar: Long,
        grandJama: Long,
        customerCount: Int,
        startY: Float
    ) {
        val rect = RectF(MARGIN, startY + 4, PAGE_WIDTH - MARGIN, startY + 44f)
        paint.color = Color.rgb(254, 243, 199)
        canvas.drawRoundRect(rect, 4f, 4f, paint)

        val y = startY + 26f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 10f
        paint.color = Color.rgb(180, 83, 9)

        canvas.drawText("सक्रिय ग्राहक: $customerCount", MARGIN + 12, y, paint)
        canvas.drawText("कुल उधार: ${CurrencyUtils.formatPaiseToRupees(grandUdhar)}", MARGIN + 140, y, paint)
        canvas.drawText("कुल जमा: ${CurrencyUtils.formatPaiseToRupees(grandJama)}", MARGIN + 330, y, paint)
    }

    private fun drawPageFooter(canvas: Canvas, paint: Paint, pageNum: Int) {
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 8f
        paint.color = Color.GRAY
        val footerText = "Digital Bahi-Khata | Page $pageNum"
        canvas.drawText(footerText, MARGIN, PAGE_HEIGHT - 15f, paint)
    }

    private fun savePdfToFile(context: Context, document: PdfDocument, fileName: String): File? {
        return try {
            val reportsDir = File(context.cacheDir, "reports")
            if (!reportsDir.exists()) reportsDir.mkdirs()
            val file = File(reportsDir, fileName)
            val outputStream = FileOutputStream(file)
            document.writeTo(outputStream)
            outputStream.flush()
            outputStream.close()
            document.close()
            file
        } catch (_: Exception) {
            document.close()
            null
        }
    }

    /**
     * Share or open the generated PDF with external apps (WhatsApp, PDF Viewer, etc.)
     */
    fun sharePdf(context: Context, pdfFile: File, title: String = "खाता रिपोर्ट PDF") {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                pdfFile
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, title)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, title))
        } catch (_: Exception) {
        }
    }
}
