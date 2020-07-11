package jp.gr.java_conf.hangedman.mmd.mesh_if

interface MeshParser {
    // Meshを実装しているクラスの実体を返す
    fun <T: Mesh> parse(meshPath: String): T
}