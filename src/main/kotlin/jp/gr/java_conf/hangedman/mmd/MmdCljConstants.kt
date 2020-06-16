package jp.gr.java_conf.hangedman.mmd

import org.joml.Matrix4f

object MmdCljConstants {

    const val width = 640
    const val height = 480
    const val title = "Load PMD file testing"

    // 頂点シェーダのソース
    const val vertexSource = """
        #version 330
        
        // 頂点情報
        in vec3 position;
        in vec3 color;
        
        // 次のフラグメントシェーダーに渡す頂点カラー
        out vec3 vertexColor;
        
        // プログラムから指定されるグローバルGLSL変数
        uniform mat4 model;
        uniform mat4 view;
        uniform mat4 projection;
        
        void main() {
            // フラグメントシェーダーには頂点の色をそのまま渡す
            vertexColor = color;
            // ModelViewProjection行列を求める
            mat4 mvp = projection * view * model;
            // gl_Positionが最終的な頂点座標
            gl_Position = mvp * vec4(position, 1.0);
        }
    """

    // フラグメントシェーダのソース
    const val fragmentSource = """
        #version 330
        
        // 頂点シェーダーから渡された頂点カラー
        in vec3 vertexColor;
        // フラグメントシェーダから出力する色
        out vec4 fragColor;
        
        void main() {
            // 頂点カラーをそのまま出力
            fragColor = vec4(vertexColor, 1.0);
        }
    """

}