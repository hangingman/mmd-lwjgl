package jp.gr.java_conf.hangedman.mmd.shader

object ModelShader {
    // 頂点シェーダのソース
    const val modelVertexSource = """
        #version 330
        
        // 頂点情報
        in vec3 position;
        in float alpha;
        in vec3 diffuseColor;
        in vec3 ambientColor;
        in vec3 specularColor;
        in vec3 normal;
        in float shininess;
        in int isEdge;
        in vec2 vUV;
        in float vTexLayer;
        in float vSphereMode;
        
        // 次のフラグメントシェーダーに渡す頂点カラー
        out float vAlpha;
        out vec3 vDiffuseColor;
        out vec3 vAmbientColor;
        out vec3 vSpecularColor;
        out vec3 vNormal;
        out vec3 vPosition;
        out float vShininess;
        out float vIsEdge;
        out vec2 UV;
        out float texLayer;
        out float fSphereMode;
        
        // プログラムから指定されるグローバルGLSL変数
        uniform mat4 model;
        uniform mat4 view;
        uniform mat4 projection;
        uniform float uEdgeSize;
        
        void main() {
            // ModelViewProjection行列を求める
            mat4 mvp = projection * view * model;
            
            vAlpha = alpha;
            vDiffuseColor = diffuseColor;
            vAmbientColor = ambientColor;
            vSpecularColor = specularColor;
            vNormal = normal;
            vPosition = position;
            vShininess = shininess;
            vIsEdge = isEdge;
            UV = vUV;
            texLayer = vTexLayer;
            fSphereMode = vSphereMode;
            
            if (isEdge == 1)
            {
                vec4 p0 = mvp * vec4(position, 1.0);
                vec4 p1 = mvp * vec4(position + normal, 1.0);
                vec4 norm = normalize(p1 - p0);
                gl_Position = p0 + norm * uEdgeSize / 10.0f;
                return;
            }
            
            // gl_Positionが最終的な頂点座標
            gl_Position = mvp * vec4(position, 1.0);
        }
    """

    // フラグメントシェーダのソース
    const val modelFragmentSource = """
        #version 330
        
        uniform vec3 uLightPosition;
        uniform vec3 uEdgeColor;
        uniform sampler2DArray tex;
        
        in float vAlpha;
        in vec3 vDiffuseColor;
        in vec3 vAmbientColor;
        in vec3 vSpecularColor;
        in vec3 vNormal;
        in vec3 vPosition;
        in float vShininess;
        in float vIsEdge;
        in vec2 UV;
        in float texLayer;
        in float fSphereMode;
        
        out vec4 fragColor;   // フラグメントシェーダから出力する色
        
        vec4 getSphereColor(int sphereMode) {
            vec4 sphereColor = vec4(0, 0, 0, 0);
          
            if (sphereMode > 0) {
                vec2 sphereCoord = 0.5 * (1.0 + vec2(1.0, -1.0) * normalize(vNormal).xy);
                sphereColor = texture(tex, vec3(UV, texLayer));
            }
          
            return sphereColor;
        }
        
        void main() {
            float ambientStrength = 0.5;
            float diffuseStrength = 0.5;
            float specularStrength = 0.5;

            vec3 lightDirection = normalize(uLightPosition - vPosition);
            vec3 normal = normalize(vNormal);
            vec3 viewPosition = vec3(0.0, 0.0, 0.0);  // たぶんモデルの位置？
            vec3 viewDirection = normalize(viewPosition - vPosition); // (model pos - vertex pos) を正規化
            
            vec3 ambientColor = ambientStrength * vAmbientColor;
            vec3 diffuseColor = diffuseStrength * max(0.0, dot(normal, lightDirection)) * vDiffuseColor;
            vec3 reflectDirection = reflect(-lightDirection, normal); 
            vec3 specularColor = specularStrength * pow(max(dot(viewDirection, reflectDirection), 0.0), vShininess) * vSpecularColor;
            
            // スフィアマップの種別
            int sphereMode = int(fSphereMode);

            if (vIsEdge == 1)
            {
                // エッジの描画
                fragColor = vec4(uEdgeColor, vAlpha);
                return;
            }
            if((UV[0]!=0 && UV[1]!=0))
            {
                // テクスチャ使用
                vec4 textureColor = texture(tex, vec3(UV, texLayer));
                vec4 sphereColor = getSphereColor(sphereMode);
                
                //if (sphereMode < 0) {
                    // テクスチャのみ
                    fragColor = textureColor;
                    return;
                /** } else if (sphereMode == 1) {
                    // スフィアマップ(テカリ/乗算) と rgbColor
                    fragColor = sphereColor * rgbColor;
                    return;
                } else if (sphereMode == 2) {
                    // スフィアマップ(テカリ/加算) と rgbColor
                    fragColor = sphereColor + rgbColor;
                    return;
                }
                return;
                */
            }
            
            fragColor = vec4(ambientColor + diffuseColor + specularColor, vAlpha);
            return;
        }
    """
}