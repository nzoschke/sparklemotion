package baaahs.gl.result

import baaahs.Color
import baaahs.fixtures.Fixture
import baaahs.fixtures.PixelArrayFixture
import baaahs.gl.GlContext
import baaahs.io.ByteArrayWriter
import baaahs.sm.brain.proto.Pixels
import baaahs.visualizer.remote.RemoteVisualizers
import com.danielgergely.kgl.ByteBuffer
import com.danielgergely.kgl.GL_RGBA
import com.danielgergely.kgl.GL_UNSIGNED_BYTE

object ColorResultType : ResultType<ColorResultType.Buffer> {
    private val renderPixelFormat: Int = GlContext.GL_RGBA8
    override val readFormat: Int
        get() = GL_RGBA
    override val readType: Int
        get() = GL_UNSIGNED_BYTE
    override val stride: Int
        get() = 4

    override fun createResultBuffer(gl: GlContext, index: Int): Buffer {
        return Buffer(gl, index)
    }

    class Buffer(gl: GlContext, resultIndex: Int) : ResultBuffer(gl, resultIndex, ColorResultType, renderPixelFormat) {
        private lateinit var byteBuffer: ByteBuffer

        override val cpuBuffer: com.danielgergely.kgl.Buffer
            get() = byteBuffer

        override fun resizeBuffer(size: Int) {
            byteBuffer = ByteBuffer(size * stride)
        }

        operator fun get(componentIndex: Int): Color {
            val offset = componentIndex * stride

            return Color(
                red = byteBuffer[offset],
                green = byteBuffer[offset + 1],
                blue = byteBuffer[offset + 2],
                alpha = byteBuffer[offset + 3]
            )
        }

        override fun getFixtureView(fixture: Fixture, bufferOffset: Int): ColorFixtureResults {
            return ColorFixtureResults(this, bufferOffset, fixture)
        }
    }

    class ColorFixtureResults(
        private val buffer: Buffer,
        componentOffset: Int,
        private val fixture: Fixture,
    ) : FixtureResults(componentOffset, fixture.componentCount), Pixels {
        private val transport = fixture.transport

        override val size: Int
            get() = componentCount

        private val fixtureConfig = fixture as PixelArrayFixture
        private val pixelFormat = fixtureConfig.pixelFormat
        private val bytesPerPixel = pixelFormat.channelsPerPixel

        override operator fun get(i: Int): Color = buffer[componentOffset + i]

        override fun set(i: Int, color: Color) = TODO("not implemented")

        override fun set(colors: Array<Color>) = TODO("not implemented")

        override fun iterator(): Iterator<Color> {
            return iterator {
                for (i in 0 until componentCount) yield(get(i))
            }
        }

        override fun send(remoteVisualizers: RemoteVisualizers) {
            transport.deliverComponents(componentCount, bytesPerPixel) { i, buf ->
                pixelFormat.writeColor(this[i], buf)
            }

            val remoteVisualizersBytes by lazy {
                val buf = ByteArrayWriter()
                for (i in 0 until componentCount) {
                    pixelFormat.writeColor(this[i], buf)
                }
                buf.toBytes()
            }
            remoteVisualizers.sendFrameData(fixture.modelEntity) { out ->
                out.writeInt(fixture.componentCount)
                out.writeBytes(remoteVisualizersBytes)
            }
        }
    }
}