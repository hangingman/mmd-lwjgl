package jp.gr.java_conf.hangedman.mmd

import java.lang.IllegalStateException

object MmdLwjglConstants {

    const val width = 800
    const val height = 640
    const val title = "Load PMD file testing"

    // 頂点シェーダのソース
    const val vertexSource = """
        #version 330
        
        // 頂点情報
        in vec3 position;
        in float alpha;
        in vec3 diffuseColor;
        in vec3 ambientColor;
        in vec3 specularColor;
        in vec3 normal;
        
        // 次のフラグメントシェーダーに渡す頂点カラー
        out float vAlpha;
        out vec3 vDiffuseColor;
        out vec3 vAmbientColor;
        out vec3 vSpecularColor;
        out vec3 vNormal;
        out vec3 vPosition;
        
        // プログラムから指定されるグローバルGLSL変数
        uniform mat4 model;
        uniform mat4 view;
        uniform mat4 projection;
        
        void main() {
            // フラグメントシェーダーには頂点の色をそのまま渡す
            //vertexDiffuseColor = diffuseColor;
            // ModelViewProjection行列を求める
            mat4 mvp = projection * view * model;
            // m逆転置行列 (Model Inverse Transposeの略)
            mat4 mit = transpose(inverse(model));
            //vec3 n = normalize(mat3(mit) * normal); // 法線のm変換
            //float nl = clamp(dot(n, 0.0), 0.0, 1.0); // 法線とライトの内積を算出
            //vec3 c = diffuseColor.rgb * nl; // 最終色を算出 
            //c = clamp(c, 0.0, 1.0); // 0.0 ~ 1.0に色を収める
            
            //vertexDiffuseColor = vec4(c, diffuseColor.a);
            
            vAlpha = alpha;
            vDiffuseColor = diffuseColor;
            vAmbientColor = ambientColor;
            vSpecularColor = specularColor;
            vNormal = normal;
            vPosition = position;
            
            // gl_Positionが最終的な頂点座標
            gl_Position = mvp * vec4(position, 1.0);
        }
    """

    // フラグメントシェーダのソース
    const val fragmentSource = """
        #version 330
        
        uniform vec3 uLightPosition;
        
        in float vAlpha;
        in vec3 vDiffuseColor;
        in vec3 vAmbientColor;
        in vec3 vSpecularColor;
        in vec3 vNormal;
        in vec3 vPosition;
        
        out vec4 fragColor;   // フラグメントシェーダから出力する色
        
        void main() {
            float ambientStrength = 0.5;
            float diffuseStrength = 0.5;
            float specularStrength = 0.5;

            vec3 lightDirection = normalize(uLightPosition - vPosition);
            vec3 normal = normalize(vNormal);
            
            vec3 ambientColor = ambientStrength * vAmbientColor;
            vec3 diffuseColor = diffuseStrength * max(0.0, dot(normal, lightDirection)) * vDiffuseColor;
            vec3 specularColor = specularStrength * vSpecularColor;
            vec3 reflectDirection = reflect(-lightDirection, normal);
    
            fragColor = vec4(ambientColor + diffuseColor + specularColor, vAlpha);
        }
    """
}

enum class VboIndex(val asInt: Int) {
    VERTEX(0),
    ALPHA(1),
    DIFFUSE_COLOR(2),
    AMBIENT_COLOR(3),
    SPECULAR_COLOR(4),
    NORMAL(5);

    fun elementSize(): Int {
        return when(this) {
            VERTEX -> 3
            ALPHA -> 1
            DIFFUSE_COLOR  -> 3
            AMBIENT_COLOR -> 3
            SPECULAR_COLOR -> 3
            NORMAL -> 3
            else -> throw IllegalStateException("Invalid VboIndex Enum")
        }
    }
}

