package clearmap.frontend.tiles

import clearmap.backend.BackendDI
import clearmap.backend.IMasterService
import clearmap.backend.tiles.ITileService
import clearmap.backend.tiles.TileSelection
import com.jogamp.opengl.GLAutoDrawable
import com.jogamp.opengl.GLCapabilities
import com.jogamp.opengl.GLEventListener
import com.jogamp.opengl.GLProfile
import com.jogamp.opengl.awt.GLJPanel
import rb.glow.Colors
import rb.glow.Composite
import rb.glow.drawer
import rb.glow.gl.GLGraphicsContext
import rb.owl.bindable.addObserver
import rbJvm.glow.jogl.JOGLProvider
import sgui.components.IComponent
import sgui.components.IComponentProvider
import sgui.components.crossContainer.ICrossPanel
import sguiSwing.components.SwComponent
import sguiSwing.hybrid.Hybrid
import spirite.specialRendering.GLSpecialDrawer
import java.io.File

class TileView(
    private val _ui: IComponentProvider,
    private val _panel: ICrossPanel,
    private val _tileSvc : ITileService = BackendDI.tileSvc.value
): IComponent by _panel
{
    constructor( ui: IComponentProvider)
            : this(ui, ui.CrossPanel())

    val label = _ui.Label("TileSets")
    val drawView = TileDrawView(_tileSvc)

    init /* Layout */ {
        _panel.setLayout {
            rows.add(label)
            rows.add(drawView.component)
            rows.flex = 1f
        }
    }


    init /* Interaction */ {
    }

    // Binding Contracts
    val tileObsK = _tileSvc.currentTileBind.addObserver { _, _ -> drawView.component.redraw() }
    val tileSelObsK = _tileSvc.tileSelectionBind.addObserver{ _, _ -> drawView.component.redraw()  }
}

class TileDrawView(private val _service : ITileService) {
    val canvas = GLJPanel(GLCapabilities(GLProfile.getDefault()))
    val component: IComponent = SwComponent(canvas)

    init /* Interaction */ {
        component.onMouseClick += { evt ->
            val currentTileSet = _service.currentTile
            if( currentTileSet != null){
                val region = currentTileSet.tiles
                    .firstOrNull{ it.contains(evt.point.x, evt.point.y)}
                if( region != null)
                    _service.tileSelection = TileSelection(currentTileSet, region)
            }
        }
    }

    val listener = object  : GLEventListener {
        override fun reshape(drawable: GLAutoDrawable, x: Int, y: Int, width: Int, height: Int) { }
        override fun dispose(drawable: GLAutoDrawable) { }

        override fun display(drawable: GLAutoDrawable) {
            val w = drawable.surfaceWidth
            val h = drawable.surfaceHeight

            val gle = Hybrid.gle
            val glgc = GLGraphicsContext(w, h, false, gle, true)

            JOGLProvider.gl2 = drawable.gl.gL2
            gle.setTarget(null)
            glgc.clear()

            val gl = gle.gl
            gl.viewport(0, 0, w, h)

            glgc.color = Colors.RED

            val specialDrawer = GLSpecialDrawer(glgc)
            specialDrawer.drawTransparencyBg(0, 0, w, h, 8)

            val tile = _service.currentTile
            if( tile != null){
                glgc.alpha = 1f
                glgc.renderImage(tile.image, 0.0, 0.0)
                Hybrid.imageIO.saveImage(tile.image, File("C:\\bucket\\x2.png"))
            }
            val tileSel = _service.tileSelection
            if( tileSel != null){
                glgc.alpha = 0.3f
                glgc.color = Colors.YELLOW
                glgc.drawer.fillRect(tileSel.region)
            }

            JOGLProvider.gl2 = null
        }

        override fun init(drawable: GLAutoDrawable) {
            // Disassociate default workspace and assosciate the workspace from the GLEngine
            //	(so they can share resources)
            val primaryContext = JOGLProvider.context

            val unusedDefaultContext = drawable.context
            unusedDefaultContext.makeCurrent()
            drawable.setContext( null, true)

            val subContext = drawable.createContext( primaryContext)
            subContext?.makeCurrent()
            drawable.setContext(subContext, true)
        }
    }

    init {
        canvas.skipGLOrientationVerticalFlip = true
        canvas.addGLEventListener(listener)
    }

}