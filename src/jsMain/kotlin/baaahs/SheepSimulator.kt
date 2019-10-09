package baaahs

import baaahs.geom.Vector3F
import baaahs.proto.Ports
import baaahs.shows.AllShows
import baaahs.sim.FakeDmxUniverse
import baaahs.sim.FakeFs
import baaahs.sim.FakeMediaDevices
import baaahs.sim.FakeNetwork
import baaahs.visualizer.SwirlyPixelArranger
import baaahs.visualizer.Visualizer
import baaahs.visualizer.VizPanel
import decodeQueryParams
import kotlinx.coroutines.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import org.w3c.dom.WebSocket
import kotlin.browser.document
import kotlin.browser.window
import kotlin.js.Date

class SheepSimulator {
    private val display = JsDisplay()
    private val network = FakeNetwork(display = display.forNetwork())
    private val dmxUniverse = FakeDmxUniverse()
    private val sheepModel = SheepModel().apply { load() }
    private val shows = AllShows.allShows
    private val visualizer = Visualizer(sheepModel, display.forVisualizer())
    private val fs = FakeFs()
    private val beatSource: BeatSource = BridgedBeatSource("${window.location.hostname}:${Ports.SIMULATOR_BRIDGE_TCP}")
    private val pinky = Pinky(sheepModel, shows, network, dmxUniverse, beatSource, JsClock(), fs,
        PermissiveFirmwareDaddy(), display.forPinky(),
        prerenderPixels = true)

    fun start() = doRunBlocking {
        val queryParams = decodeQueryParams(document.location!!)

        pinkyScope.launch { pinky.run() }

        val launcher = Launcher(document.getElementById("launcher")!!)
        launcher.add("Web UI") {
            val webUiClientLink = network.link()
            val pubSub = PubSub.Client(webUiClientLink, pinky.address, Ports.PINKY_UI_TCP).apply {
                install(gadgetModule)
            }
            document.asDynamic().createUiApp(pubSub)
        }.also { delay(1000); it.click() }

        launcher.add("Mapper") {
            val mapperUi = JsMapperUi(visualizer)
            val mediaDevices = FakeMediaDevices(visualizer)
            val mapper = Mapper(network, sheepModel, mapperUi, mediaDevices, pinky.address)
            mapperScope.launch { mapper.start() }

            mapperUi
        }

        val pixelDensity = queryParams.getOrElse("pixelDensity") { "0.2" }.toFloat()
        val pixelSpacing = queryParams.getOrElse("pixelSpacing") { "3" }.toFloat()
        val pixelArranger = SwirlyPixelArranger(pixelDensity, pixelSpacing)
        var totalPixels = 0

        sheepModel.panels.sortedBy(SheepModel.Panel::name).forEachIndexed { index, panel ->
            //            if (panel.name != "17L") return@forEachIndexed

            val vizPanel = visualizer.addPanel(panel)
            val pixelPositions = pixelArranger.arrangePixels(vizPanel)
            vizPanel.vizPixels = VizPanel.VizPixels(vizPanel, pixelPositions)

            totalPixels += pixelPositions.size
            document.getElementById("visualizerPixelCount").asDynamic().innerText = totalPixels.toString()

            // This part is cheating... TODO: don't cheat!
            val pixelLocations = vizPanel.getPixelLocationsInModelSpace()!!.map {
                Vector3F(it.x.toFloat(), it.y.toFloat(), it.z.toFloat())
            }
            pinky.providePixelMapping_CHEAT(panel, pixelLocations)

            val brain = Brain("brain//$index", network, display.forBrain(), vizPanel.vizPixels ?: NullPixels)
            pinky.providePanelMapping_CHEAT(BrainId(brain.id), panel)
            brainScope.launch { randomDelay(1000); brain.run() }
        }

        sheepModel.eyes.forEach { eye ->
            visualizer.addMovingHead(eye, dmxUniverse)
        }

//        val users = storage.users.transaction { store -> store.getAll() }
//        println("users = ${users}")

        doRunBlocking {
            delay(200000L)
        }
    }

    object NullPixels : Pixels {
        override val size = 0

        override fun get(i: Int): Color = Color.BLACK
        override fun set(i: Int, color: Color) {}
        override fun set(colors: Array<Color>) {}
    }

    private val pinkyScope = CoroutineScope(Dispatchers.Main)
    private val brainScope = CoroutineScope(Dispatchers.Main)
    private val mapperScope = CoroutineScope(Dispatchers.Main)
}

class JsClock : Clock {
    override fun now(): Time = Date.now() / 1000.0
}

class BridgedBeatSource(private val url: String) : BeatSource {
    private val logger = Logger("BridgedBeatSource")
    private val json = Json(JsonConfiguration.Stable)
    private val defaultBpm = BeatData(0.0, 500, confidence = 1f)
    private val l = window.location
    private lateinit var webSocket: WebSocket

    private var beatData = BeatData(0.0, 0, confidence = 0f)
    private var everConnected = false

    override fun getBeatData(): BeatData = beatData

    init {
        connect()
    }

    private fun connect() {
        webSocket = WebSocket("${if (l.protocol == "https:") "wss:" else "ws:"}//$url/bridge/beatSource")

        webSocket.onopen = {
            everConnected = true
            logger.info { "Connected to simulator bridge." }
        }

        webSocket.onmessage = {
            val buf = it.data as String
            logger.debug { "Received $buf" }
            beatData = json.parse(BeatData.serializer(), buf)
            null
        }

        webSocket.onerror = {
            if (!everConnected) {
                logger.error { "Couldn't connect to simulator bridge; falling back to 120bpm: $it" }
                beatData = defaultBpm
            } else {
                logger.error { "WebSocket error: $it" }
            }
        }

        webSocket.onclose = {
            if (everConnected) {
                logger.error { "Lost connection to simulator bridge; falling back to 120bpm: $it" }
                beatData = defaultBpm

                GlobalScope.launch {
                    delay(1000)
                    logger.info { "Attempting to reconnect to simulator bridge..." }
                    connect()
                }
            }
        }
    }
}
