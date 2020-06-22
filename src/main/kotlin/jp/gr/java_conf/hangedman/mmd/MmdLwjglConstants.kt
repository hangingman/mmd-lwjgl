package jp.gr.java_conf.hangedman.mmd

object MmdLwjglConstants {

    const val width = 800
    const val height = 640
    const val title = "Load PMD file testing"

    // 頂点シェーダのソース
    const val vertexSource = """
        #version 330
        
        // 頂点情報
        in vec3 position;
        in vec4 color;
        in vec3 normal;
        
        // 次のフラグメントシェーダーに渡す頂点カラー
        out vec4 vertexColor;
        
        // プログラムから指定されるグローバルGLSL変数
        uniform mat4 model;
        uniform mat4 view;
        uniform mat4 projection;
        
        void main() {
            // フラグメントシェーダーには頂点の色をそのまま渡す
            //vertexColor = color;
            // ModelViewProjection行列を求める
            mat4 mvp = projection * view * model;
            // m逆転置行列 (Model Inverse Transposeの略)
            mat4 mit = transpose(inverse(model));
            //vec3 n = normalize(mat3(mit) * normal); // 法線のm変換
            //float nl = clamp(dot(n, 0.0), 0.0, 1.0); // 法線とライトの内積を算出
            //vec3 c = color.rgb * nl; // 最終色を算出 
            //c = clamp(c, 0.0, 1.0); // 0.0 ~ 1.0に色を収める
            
            //vertexColor = vec4(c, color.a);
            vertexColor = color;
            
            // gl_Positionが最終的な頂点座標
            gl_Position = mvp * vec4(position, 1.0);
        }
    """

    // フラグメントシェーダのソース
    const val fragmentSource = """
        #version 330
        
        uniform vec3 uLightPosition;
        in vec4 vertexColor;  // 頂点シェーダーから渡された頂点カラー
        out vec4 fragColor;   // フラグメントシェーダから出力する色
        
        void main() {
            // 頂点カラーをそのまま出力
            fragColor = vertexColor;
        }
    """

}