package jp.gr.java_conf.hangedman.lwjgl

import org.joml.Matrix4f
import org.joml.Vector3f
import org.lwjgl.opengl.GL20.glGetUniformLocation
import org.lwjgl.opengl.GL20.glUniformMatrix4fv

object ModelViewProjection {
    fun updateMVP(shader: Int, fov: Float, position: Vector3f) {
        // Model行列(描画対象のモデルの座標からOpenGLのワールド座標への相対値)
        // 頂点シェーダーのグローバルGLSL変数"model"に設定
        val uniModel = glGetUniformLocation(shader, "model")
        // updateメソッドで求めた回転行列をグローバルGLSL変数に設定
        glUniformMatrix4fv(uniModel, false, Matrix4f().value())

        // View行列(OpenGLのワールド座標からカメラの座標への相対値)
        // 頂点シェーダーのグローバルGLSL変数"view"に設定
        val uniView = glGetUniformLocation(shader, "view")
        val viewMatrix = Matrix4f().setLookAt(
                position.x, position.y, position.z,  // ワールド空間でのカメラの位置
                0f, 0f, 0f, // ワールド空間での見たい位置
                0f, 1f, 0f
        )
        glUniformMatrix4fv(uniView, false, viewMatrix.value())

        // Projection行列(カメラの座標から、映し出される（射影）ものへの相対値)
        // 頂点シェーダーのグローバルGLSL変数"projection"に設定
        val projectionMatrix = Matrix4f().createProjectionMatrix(fov)
        val uniProjection = glGetUniformLocation(shader, "projection")
        glUniformMatrix4fv(uniProjection, false, projectionMatrix.value())
    }

    fun updateMVP(shader: Int, model: Matrix4f, view: Matrix4f, projection: Matrix4f) {

        // Model行列(描画対象のモデルの座標からOpenGLのワールド座標への相対値)
        val uniModel = glGetUniformLocation(shader, "model")
        glUniformMatrix4fv(uniModel, false, model.value())

        // View行列(OpenGLのワールド座標からカメラの座標への相対値)
        val uniView = glGetUniformLocation(shader, "view")
        glUniformMatrix4fv(uniView, false, view.value())

        // Projection行列(カメラの座標から、映し出される（射影）ものへの相対値)
        val uniProjection = glGetUniformLocation(shader, "projection")
        glUniformMatrix4fv(uniProjection, false, projection.value())
    }
}