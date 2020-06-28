package jp.gr.java_conf.hangedman.mmd.shader

object AxisShader {
    // 頂点シェーダのソース
    const val axisVertexSource = """
        #version 330
        
        // 頂点情報
        in vec3 position;
        in vec3 colors;
        
        out vec3 vColors;
        
        // プログラムから指定されるグローバルGLSL変数
        uniform mat4 model;
        uniform mat4 view;
        uniform mat4 projection;
        
        void main() {
            mat4 mvp = projection * view * model;
            vColors = colors;
            
            gl_Position = vec4(position, 1.0);
            //gl_Position = mvp * vec4(position, 1.0);
        }
    """

    // フラグメントシェーダのソース
    const val axisFragmentSource = """
        #version 330

        in vec3 vColors;
        
        out vec4 fragColor;   // フラグメントシェーダから出力する色
        
        void main() {
            fragColor = vec4(vColors, 0.0);
        }
    """
}