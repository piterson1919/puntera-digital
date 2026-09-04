package com.punteradigital.inventory.domain.usecase

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Environment
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

class PdfGeneratorUseCase @Inject constructor(
    @ApplicationContext private val context: Context
) {

    /**
     * Genera un PDF en formato carta con etiquetas para Cajas Master y Unidades Individuales.
     */
    fun generateBatchPdf(model: String, batch: String, uuids: List<String>): File? {
        val pdfDocument = PdfDocument()
        val paint = Paint()
        val titlePaint = Paint().apply {
            textSize = 14f
            isFakeBoldText = true
        }
        val labelPaint = Paint().apply {
            textSize = 9f
            isFakeBoldText = true
        }
        val subLabelPaint = Paint().apply {
            textSize = 7f
        }
        val masterPaint = Paint().apply {
            textSize = 12f
            isFakeBoldText = true
            color = Color.BLUE
        }

        // Dimensiones carta (72 DPI) -> 612 x 792 puntos
        val pageWidth = 612
        val pageHeight = 792
        
        // Configuración de cuadrícula (4 columnas x 5 filas = 20 por página)
        val cols = 4
        val rows = 5
        val margin = 40f
        val cellWidth = (pageWidth - 2 * margin) / cols
        val cellHeight = (pageHeight - 2 * margin) / rows
        val qrSize = (cellWidth * 0.7f).toInt()

        // Identificar Master Boxes únicas para imprimir sus etiquetas primero
        // Se asume que el MasterBoxId está embebido o relacionado. 
        // En este flujo, sacaremos los Master IDs únicos de la lista si existen, 
        // pero como la función recibe solo 'uuids' (unidades), 
        // necesitamos asegurar que el PDF incluya las etiquetas de caja.
        
        // OPCIÓN: Si los UUIDs son solo de unidades, intentaremos deducir los MASTER 
        // o simplemente imprimiremos lo que nos pasen. 
        // ACTUALIZACIÓN: Modificamos la firma para aceptar Master UUIDs si es necesario, 
        // o simplemente procesamos la lista tal cual asumiendo que incluye ambos.
        
        var currentUuidIndex = 0
        var pageNumber = 1

        while (currentUuidIndex < uuids.size) {
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas

            // Encabezado de página
            canvas.drawText("Lote: $batch | Modelo: $model | Página $pageNumber", margin, margin / 2, titlePaint)

            for (row in 0 until rows) {
                for (col in 0 until cols) {
                    if (currentUuidIndex >= uuids.size) break

                    val x = margin + col * cellWidth
                    val y = margin + row * cellHeight

                    val uuid = uuids[currentUuidIndex]
                    val isMaster = uuid.startsWith("MASTER-")
                    val shortId = uuid.takeLast(8).uppercase() 
                    val qrBitmap = generateQrBitmap(uuid, qrSize)

                    if (qrBitmap != null) {
                        // Dibujar QR
                        val qrX = x + (cellWidth - qrSize) / 2
                        val qrY = y + 10f
                        canvas.drawBitmap(qrBitmap, qrX, qrY, paint)
                        
                        // Dibujar textos debajo del QR
                        val textX = x + 10f
                        if (isMaster) {
                            canvas.drawText("CAJA MASTER", textX, qrY + qrSize + 12f, masterPaint)
                            canvas.drawText("MOD: $model", textX, qrY + qrSize + 26f, labelPaint)
                            canvas.drawText("ID: $shortId", textX, qrY + qrSize + 38f, labelPaint)
                        } else {
                            canvas.drawText("UNIDAD INDIV.", textX, qrY + qrSize + 12f, labelPaint)
                            canvas.drawText("MOD: $model", textX, qrY + qrSize + 24f, labelPaint)
                            canvas.drawText("ID: $shortId", textX, qrY + qrSize + 36f, labelPaint)
                        }
                        canvas.drawText("UUID: ${uuid.take(20)}...", textX, qrY + qrSize + 46f, subLabelPaint)
                    }

                    currentUuidIndex++
                }
            }

            pdfDocument.finishPage(page)
            pageNumber++
        }

        // Guardar archivo
        return try {
            val fileName = "Etiquetas_${batch}_${System.currentTimeMillis()}.pdf"
            val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName)
            pdfDocument.writeTo(FileOutputStream(file))
            pdfDocument.close()
            file
        } catch (e: Exception) {
            e.printStackTrace()
            pdfDocument.close()
            null
        }
    }

    private fun generateQrBitmap(content: String, size: Int): Bitmap? {
        return try {
            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size)
            val width = bitMatrix.width
            val height = bitMatrix.height
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setPixel(x, y, if (bitMatrix.get(x, y)) Color.BLACK else Color.WHITE)
                }
            }
            bitmap
        } catch (e: Exception) {
            null
        }
    }
}
