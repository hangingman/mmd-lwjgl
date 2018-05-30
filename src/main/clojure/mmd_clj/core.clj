(ns mmd-clj.core
  (:require
   [clojure.java.io :as io]
   [mmd-clj.pmd :as pmd])
  (:import
   [org.apache.commons.lang3.time StopWatch DurationFormatUtils]
   [org.lwjgl Version]
   [org.lwjgl BufferUtils]
   [org.lwjgl.system MemoryStack]
   [org.lwjgl.opengl GL GL11 GL15 GL20 GL30 GLUtil]
   [org.lwjgl.glfw GLFW]
   [org.joml Matrix4f Vector3f])
  (:use
   [clojure.tools.logging]
   [clojure.java.io :only [input-stream]]
   [org.clojars.smee.binary.core]))

(def vertex-source
  (clojure.string/join "\n"
                       ["#version 150"
                        ""
                        "in  vec3 vertex;"
                        "uniform  mat4 view;"
                        "uniform  mat4 model;"
                        "uniform  mat4 projection;"
                        ""
                        "void main () {"
                        "    gl_Position = projection * view * model * vec4(vertex, 1);"
                        "}"]))

(def fragment-source
  (clojure.string/join "\n"
                       ["#version 150"
                        ""
                        "out vec4 fragColor;"
                        ""
                        "void main () {"
                        "    fragColor = vec4(0.0, 1.0, 0.0, 1.0);"
                        ;; " gl_FragColor = vec4(0.1,0.4,0.9,1.0);" <-- エラー発生
                        "}"]))

(def vertex
  (float-array [(- 1.0)    1.0  (- 1.0)
                1.0     1.0  (- 1.0)
                1.0  (- 1.0) (- 1.0)
                (- 1.0) (- 1.0) (- 1.0)]))

(def index
  (int-array [0 1 2 0 2 3]))

(def width 640)
(def height 480)
(def title "Load PMD file testing")

(defn mat-proj []
  (let [mat-proj (new Matrix4f)]
    (doto mat-proj
      (.frustum (/ (- width) 2.0) (/ width 2.0) (/ (- height) 2.0) (/ height 2.0) (- 100.0) 100.0))
    mat-proj))

(defn mat-model []
  (let [mat-model (new Matrix4f)]
    (doto mat-model
      (.translate 120.0 (- 50.0) 50.0)
      (.scale 100.0)
      (.rotate (float (/ Math/PI 6.0)) 1.0 1.0 1.0))
    mat-model))

(defn mat-view []
  (let [mat-view (new Matrix4f)]
    (doto mat-view
      (.lookAt 0 0 200 0 0 0 0 1 0))
    mat-view))

(defn read-shader-src [shader-obj shader-src]
  (debug (format "shader source: \n%s" shader-src))
  (GL20/glShaderSource shader-obj shader-src))

(defn make-shader [vertex-source fragment-source]
  ;; シェーダーオブジェクト作成
  (let [vert-shader-obj (GL20/glCreateShader GL20/GL_VERTEX_SHADER)
        frag-shader-obj (GL20/glCreateShader GL20/GL_FRAGMENT_SHADER)]

    ;; シェーダーのソースプログラムの読み込み
    (read-shader-src vert-shader-obj vertex-source)
    (read-shader-src frag-shader-obj fragment-source)

    ;; バーテックスシェーダーのソースプログラムのコンパイル
    (GL20/glCompileShader vert-shader-obj)
    ;; フラグメントシェーダーのソースプログラムのコンパイル
    (GL20/glCompileShader frag-shader-obj)

    ;; プログラムオブジェクトの作成
    (let [shader (GL20/glCreateProgram)]
      ;; シェーダーオブジェクトのシェーダープログラムへの登録
      (GL20/glAttachShader shader vert-shader-obj)
      (GL20/glAttachShader shader frag-shader-obj)
      ;; シェーダーオブジェクトの削除
      (GL20/glDeleteShader vert-shader-obj)
      (GL20/glDeleteShader frag-shader-obj)
      ;; シェーダーにデータの位置をバインド
      (GL30/glBindFragDataLocation shader 0 "fragColor")
      ;; シェーダープログラムのリンク
      (GL20/glLinkProgram shader)
      (GL20/glUseProgram shader)
      shader)))

(defn get-current-ratio []
  (with-open [stack (MemoryStack/stackPush)]
    (let [window (GLFW/glfwGetCurrentContext)
          width (.mallocInt stack 1)
          height (.mallocInt stack 1)]
      (GLFW/glfwGetFramebufferSize window width height)
      (float (/ (.get width) (.get height))))))

(defn render [window shader v-array-id index-id attrib-vertex index-gl]
  ;; バッファのクリア
  (GL11/glClearColor 0.2 0.2 0.2 0.0)
  (GL11/glClear GL11/GL_COLOR_BUFFER_BIT)
  (GL20/glUseProgram shader)

  (GL30/glBindVertexArray v-array-id)
  (GL20/glEnableVertexAttribArray attrib-vertex)
  (GL15/glBindBuffer GL15/GL_ELEMENT_ARRAY_BUFFER index-id)

  (GL11/glDrawElements GL11/GL_TRIANGLES (count index-gl) GL11/GL_UNSIGNED_INT 0)

  (GL20/glDisableVertexAttribArray attrib-vertex)
  (GL15/glBindBuffer GL15/GL_ELEMENT_ARRAY_BUFFER 0)

  (GL30/glBindVertexArray 0)
  (GL20/glUseProgram 0)

  ;; ダブルバッファのスワップ
  (GLFW/glfwSwapBuffers window)
  (GLFW/glfwPollEvents))

;; 初期化してシェーダーを返す
(defn enter []
  ;; GLFW初期化
  (GLFW/glfwInit)
  (GLFW/glfwDefaultWindowHints)
  (GLFW/glfwWindowHint GLFW/GLFW_VISIBLE GLFW/GLFW_TRUE)
  (GLFW/glfwWindowHint GLFW/GLFW_RESIZABLE GLFW/GLFW_TRUE)
  (GLFW/glfwWindowHint GLFW/GLFW_CONTEXT_VERSION_MAJOR 3)
  (GLFW/glfwWindowHint GLFW/GLFW_CONTEXT_VERSION_MINOR 2)
  (GLFW/glfwWindowHint GLFW/GLFW_OPENGL_PROFILE GLFW/GLFW_OPENGL_CORE_PROFILE)
  (GLFW/glfwWindowHint GLFW/GLFW_OPENGL_FORWARD_COMPAT GLFW/GLFW_TRUE)

  ;; ウィンドウ生成
  (let [window (GLFW/glfwCreateWindow width height title 0 0)]
    (if-not window
      ;; 生成失敗
      (GLFW/glfwTerminate)
      (do ;; 生成OK
        (GLFW/glfwSetWindowAspectRatio window 1 1)
        (let [vidmode (GLFW/glfwGetVideoMode (GLFW/glfwGetPrimaryMonitor))]
          (GLFW/glfwSetWindowPos window
                                 (/ (- (.width vidmode) width) 2)
                                 (/ (- (.height vidmode) width) 2)))

        ;; コンテキストの作成
        (GLFW/glfwMakeContextCurrent window)
        (GLFW/glfwSwapInterval 1)
        (GL/createCapabilities)
        ;; ログをClojureに回す
        (GLUtil/setupDebugMessageCallback (log-stream :debug "OpenGL"))

        (let [shader (make-shader vertex-source fragment-source)
              attrib-vertex (GL20/glGetAttribLocation shader "vertex")
              fb (BufferUtils/createFloatBuffer 16)]

          (GL20/glUniformMatrix4fv (GL20/glGetUniformLocation shader "projection") false (.get (mat-proj) fb))
          (GL20/glUniformMatrix4fv (GL20/glGetUniformLocation shader "view")       false (.get (mat-view) fb))
          (GL20/glUniformMatrix4fv (GL20/glGetUniformLocation shader "model")      false (.get (mat-model) fb))
          (GL20/glUseProgram 0)
          [window shader attrib-vertex])))))

(defn init-vao [window attrib-vertex vertex-gl index-gl]
  ;; (GLFW/glfwSetWindowSizeCallback window (fn [window width height] (GL11/glViewport 0 0 width height)))

  (let [vertice-buffer (BufferUtils/createFloatBuffer (count vertex-gl))
        index-buffer (BufferUtils/createIntBuffer (count index-gl))
        v-array-id (GL30/glGenVertexArrays)
        v-buffer-id (GL15/glGenBuffers)
        index-id (GL15/glGenBuffers)]
    (.flip (.put vertice-buffer vertex-gl))
    (.flip (.put index-buffer index-gl))
    (GL30/glBindVertexArray v-array-id)

    ;; Vertex Buffer Object
    (GL15/glBindBuffer GL15/GL_ARRAY_BUFFER v-buffer-id)
    (GL15/glBufferData GL15/GL_ARRAY_BUFFER vertice-buffer GL15/GL_STATIC_DRAW)
    (GL20/glVertexAttribPointer attrib-vertex 3 GL11/GL_FLOAT false 0 0)

    ;; Element Buffer Object
    (GL15/glBindBuffer GL15/GL_ELEMENT_ARRAY_BUFFER index-id)
    (GL15/glBufferData GL15/GL_ELEMENT_ARRAY_BUFFER index-buffer GL15/GL_STATIC_DRAW)

    (GL15/glBindBuffer GL15/GL_ARRAY_BUFFER 0)
    (GL30/glBindVertexArray 0)
    [v-array-id index-id]))

(defn read-is [^java.io.InputStream is]
  (let [bufsize 8192
        buf (byte-array bufsize)]
    (loop [total-len 0]
      (let [n (.read is buf)]
        (cond
          (pos? n) (do
                     ;; process n bytes in buf here
                     (debug "Remaining" n " bytes...?")
                     (recur (+ total-len n)))
          :else
          (debug "Reach EOF !"))))))  ;; or whatever ret value you want

(defn mill-time-format [^long start-time ^long end-time]
  (DurationFormatUtils/formatPeriod start-time end-time "HH:mm:ss.SSS"))

(defn load-pmd-file [^String pmd-file]
  (let [stopwatch (new StopWatch)]
    (.start stopwatch)
    (debug (str "load file: " pmd-file))

    (let [in (input-stream (io/resource pmd-file))
          header (:header (decode pmd/pmd-header in))
          vertex (:vertex (decode pmd/pmd-vertex in))
          face-vertex (:face_vert_index (decode pmd/pmd-face-vertex in))
          material (:material (decode pmd/pmd-material in))
          bone (:bone (decode pmd/pmd-bone in))
          ik (:ik_data (decode pmd/pmd-ik in))
          skin (:skin_data (decode pmd/pmd-skin in))
          skin-disp (:skin_index (decode pmd/pmd-skin-disp in))
          bone-disp-names (:bone_disp_name (decode pmd/pmd-bone-disp-names in))
          bone-disp (:bone_disp (decode pmd/pmd-bone-disp in))
          en-name (decode pmd/pmd-english-name-compatibility in)
          bone-disp-names-en (:bone_name_eg (decode pmd/pmd-bone-disp-names-en in))
          skin-names-en (:skin_name_eg (decode pmd/pmd-skin-en in))
          bone-disp-fnames (:bone_disp_fname_eg (decode pmd/pmd-bone-disp-fnames-en in))
          toon-texture-names (:toon_file_name (decode pmd/pmd-toon-texture-names in))
          rigidbody (:rigidbody (decode pmd/pmd-rigidbody in))
          joint (:joint (decode pmd/pmd-joint in))]

      (debug (str
              (:magic header)
              ", version:"
              (:version header)
              ", model:"
              (:model_name header)))

      (debug (str "vertex: " (count vertex)))
      (debug (str "face-vertex: " (count face-vertex)))
      (debug (str "material: " (count material)))
      (debug (str "bone: " (count bone)))
      (debug (str "ik: " (count ik)))
      (debug (str "skin: " (count skin)))
      (debug (str "skin-disp: " (count skin-disp)))
      (debug (str "bone-disp-names: " (count bone-disp-names)))
      (debug (str "bone-disp: " (count bone-disp)))
      (debug (str "en-name: " (:model_name_eg en-name)))
      (debug (str "bone-disp-names-en: " (count bone-disp-names-en)))
      (debug (str "skin-names-en: " (count skin-names-en)))
      (debug (str "toon-texture-names: " (count toon-texture-names)))
      (debug (str "rigidbody: " (count rigidbody)))
      (debug (str "joint: " (count joint)))

      ;; debug
      (read-is in)
      ;; show time
      (.stop stopwatch)
      (debug (str "Loading time: " (mill-time-format 0 (.getTime stopwatch))  ", Hello MMD!"))

      {:header header
       :vertex vertex
       :face-vertex face-vertex
       :material material
       :bone bone
       :ik ik
       :skin skin
       :skin-disp skin-disp
       :bone-disp-names bone-disp-names
       :bone-disp bone-disp
       :en-name en-name
       :bone-disp-names-en bone-disp-names-en
       :skin-names-en skin-names-en
       :bone-disp-fnames bone-disp-fnames
       :toon-texture-names toon-texture-names
       :rigidbody rigidbody
       :joint joint})))

  (defn -main []
    (println (format "Hello, LWJGL - %s !" (Version/getVersion)))
    (println (format "       OpenGL - %s !" (GL11/GL_VERSION)))
    (println (format "       GLFW - %s !" (GLFW/glfwGetVersionString)))
    (println "Goodjob !")
    (println "")

    (let [pmd-object (load-pmd-file "HatsuneMiku.pmd")
          vertex-gl (float-array (flatten (map (fn [v] (:pos v)) (:vertex pmd-object))))
          index-gl (int-array (:face-vertex pmd-object))]

      ;; 生成した値を受け取る
      (let [[window shader attrib-vertex] (enter)
            [v-array-id index-id] (init-vao
                                   window
                                   attrib-vertex
                                   vertex-gl
                                   index-gl)]
        ;; フレームループ
        (while (not (GLFW/glfwWindowShouldClose window))
          (render
           window
           shader
           v-array-id
           index-id
           attrib-vertex
           index-gl))
        ;; GLFWの終了処理
        (GLFW/glfwTerminate))))
