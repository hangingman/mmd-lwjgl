package jp.gr.java_conf.hangedman.mmd

import org.joml.Matrix4f

object MmdCljConstants {

    const val width = 640
    const val height = 480
    const val title = "Load PMD file testing"

    const val vertexSource = """
        #version 150
        
        in  vec3 vertex;
        uniform  mat4 view;
        uniform  mat4 model;
        uniform  mat4 projection;
        
        void main () {
            gl_Position = projection * view * model * vec4(vertex, 1);
        }
    """

    const val fragmentSource = """
        #version 150
        
        out vec4 fragColor;

        void main () {
            fragColor = vec4(0.0, 1.0, 0.0, 1.0);"
            // gl_FragColor = vec4(0.1,0.4,0.9,1.0);" <-- エラー発生
        }
    """

    val matProj = Matrix4f().apply {
        this.frustum(
                (-width / 2.0).toFloat(),
                (width / 2.0).toFloat(),
                (-height / 2.0).toFloat(),
                (height / 2.0).toFloat(),
                (- 100.0).toFloat(),
                100.0F
        )
    }

    val matModel = Matrix4f().apply {
        this.translate(120.0F, -50.0F, 50.0F)
        this.scale(100.0F)
        this.rotate((Math.PI / 6.0).toFloat(), 1.0F, 1.0F, 1.0F)
    }

    val matView = Matrix4f().apply {
        this.lookAt(0F, 0F, 200F, 0F, 0F, 0F, 0F, 1F, 0F)
    }
}