package com.hika.accessibility.recognition

import android.graphics.PixelFormat
import android.graphics.Rect
import android.media.Image
import android.media.ImageReader
import android.view.Surface
import com.hika.accessibility.recognition.means.ocr.GoogleOCRer
import com.hika.core.aidl.accessibility.TemplateImageID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.nio.ByteBuffer


class ImageHandler(width: Int, height: Int, val scope: CoroutineScope) {
    // 1. Create a surface for media projection to throw
    private val imageReader: ImageReader =
        ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)

    val surface: Surface
        get() = imageReader.surface


    // 2. Get the current frame on surface (internal), meanwhile control the reading interval.
    val interval = 50L     // TODO: reading interval configuration needed
    var expirationTime: Long = -1   // -1: uninitialized
    private var frameUpdatingJob: Job? = null

    private var frame: Image? = null
        get() {
            if (System.currentTimeMillis() >= expirationTime && frameUpdatingJob == null)
                frameUpdatingJob = scope.launch {
                    imageReader.acquireLatestImage()?.run {
                        field?.close()  // do we really need this? through the official document.
                        field = this
                        expirationTime = System.currentTimeMillis() + interval
                        frameUpdatingJob == null
                    }
                    // if null, acquiring failed, the next getting request should launch the acquiring
                }
            return field
        }



    // 3. Expose the vision for outer manipulator: deprecated
    fun getRecognizable() = frame?.let{
        Recognizable(it)
    }

    class Recognizable(val currentImage: Image){
        // may change
        val width = currentImage.width
        val height = currentImage.height

        fun findOnOpenCV(templateImageIDs: Set<TemplateImageID>): Map<TemplateImageID, Array<Rect>>
            = throw NotImplementedError()

        suspend fun findOnGoogleOCRerInRangeAsync(region: Rect?)
            = (region?.run {
                    val cropWidth = this.width()
                    val cropHeight = this.height()
                    GoogleOCRer.initiateNV21Recognition(
                        cropNV21(
                            imageNV21_Array,
                            width,
                            height,
                            this.left,
                            this.top,
                            cropWidth,
                            cropHeight
                        ),
                        cropWidth,
                        cropHeight
                    )
                } ?: GoogleOCRer.initiateNV21Recognition(
                        imageNV21_Array,
                        width,
                        height
                    )
            ).await()

//        private val openCVMat by lazy { OpenCVRecognizer.getMat(currentImage) }
        private val imageNV21_Array by lazy {
            convertRGBAtoNV21_Array(currentImage) ?: throw Exception("NV21 conversion failed") }
    }

    // 4. Convert RGBA image to NV21
    companion object{
        init{
            System.loadLibrary("libyuv_converter")
        }
        private external fun convertRGBAtoNV21(
            rgbaBuffer: ByteBuffer,
            rgbaStride: Int,
            width: Int,
            height: Int,
            nv21Array: ByteArray): Boolean

        private fun convertRGBAtoNV21_Array(image: Image): ByteArray? {
            val planes = image.planes
            if (planes.isEmpty()) {
                return null
            }
            val rgbaBuffer = planes[0].buffer
            val rgbaStride = planes[0].rowStride

            val nv21Array = ByteArray(image.width * image.height * 3 / 2)

            val success = convertRGBAtoNV21(
                rgbaBuffer,
                rgbaStride,
                image.width,
                image.height,
                nv21Array)

            return if (success) nv21Array else null
        }


        // 5. Crop the image to constraint the range of recognition
        /**
         * 对NV21格式的图像进行区域裁剪
         *
         * @param nv21Data 原始NV21数据
         * @param originalWidth 原始图像宽度
         * @param originalHeight 原始图像高度
         * @param x 裁剪区域左上角X坐标
         * @param y 裁剪区域左上角Y坐标
         * @param cropWidth 裁剪区域宽度
         * @param cropHeight 裁剪区域高度
         * @return 裁剪后的NV21数据
         */
        fun cropNV21(
            nv21Data: ByteArray,
            originalWidth: Int,
            originalHeight: Int,
            x: Int,
            y: Int,
            cropWidth: Int,
            cropHeight: Int
        ): ByteArray {
            // --- 1. 起点强制在图像范围内 ---
            var cx = x.coerceAtLeast(0)
            var cy = y.coerceAtLeast(0)

            // --- 2. 宽高不要超出边界 ---
            var cw = if (cropWidth > originalWidth - cx) originalWidth - cx else cropWidth
            var ch = if (cropHeight > originalHeight - cy) originalHeight - cy else cropHeight

            // --- 3. 强制偶数对齐 ---
            cx = cx and 1.inv()
            cy = cy and 1.inv()
            cw = cw and 1.inv()
            ch = ch and 1.inv()

            // --- 4. 最终检查 ---
            if (cw <= 0 || ch <= 0) {
                throw IllegalArgumentException("Invalid crop size after adjustment: ($cw x $ch) at ($cx,$cy)")
            }

            val ySize = originalWidth * originalHeight
            val output = ByteArray(cw * ch * 3 / 2)

            // --- 5. 拷贝 Y 平面 ---
            var outPos = 0
            for (row in 0 until ch) {
                val srcPos = (cy + row) * originalWidth + cx
                System.arraycopy(nv21Data, srcPos, output, outPos, cw)
                outPos += cw
            }

            // --- 6. 拷贝 UV 平面 ---
            val uvCropStart = ySize + (cy / 2) * originalWidth + cx
            for (row in 0 until ch / 2) {
                val srcPos = uvCropStart + row * originalWidth
                System.arraycopy(nv21Data, srcPos, output, outPos, cw)
                outPos += cw
            }

            return output
        }
    }


    // 3. clean-ups
    fun release(){
        imageReader.close()
    }
}