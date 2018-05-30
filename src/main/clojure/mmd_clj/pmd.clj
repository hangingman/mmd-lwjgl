(ns mmd-clj.pmd
  (:require
   [clojure.java.io :as io])
  (:use
   [clojure.tools.logging]
   [clojure.java.io :only [input-stream]]
   [org.clojars.smee.binary.core]))



(def pmd-header
  ;; ヘッダファイル
  (compile-codec
   (ordered-map
    :header
    (ordered-map
     :magic (string "ISO-8859-1" :length 3) ;; Pmd
     :version :float-le
     :model_name (string "Windows-31J" :length 20)
     :comment (string "Windows-31J" :length 256)))))

(def pmd-vertex
  ;; 頂点リスト
  (compile-codec
   (ordered-map
    :vertex
    (repeated
     (ordered-map
      :pos (repeated :float-le :length 3)        ;; x, y, z; 座標
      :normal_vec (repeated :float-le :length 3) ;; nx, ny, nz; 法線ベクトル
      :uv (repeated :float-le :length 2)         ;; u, v; UV座標, MMDは頂点UV
      :bone_num (repeated :ushort-le :length 2)  ;; ボーン番号1、番号2; モデル変形(頂点移動)時に影響
      :bone_weight :ubyte ;; ボーン1に与える影響度; min:0 max:100 # ボーン2への影響度は、(100 - bone_weight)
      :edge_flag :ubyte)  ;; 0:通常、1:エッジ無効 ; エッジ(輪郭)が有効の場合
     ;; 頂点リストのサイズ
     :prefix :uint-le
     ))))

(def pmd-face-vertex
  ;; 面頂点リスト
  (compile-codec
   (ordered-map
    :face_vert_index (repeated :ushort-le :prefix :uint-le))))

(def pmd-material
  ;; 材質リスト
  (compile-codec
   (ordered-map
    :material
    (repeated
     (ordered-map
      :diffuse_color (repeated :float-le :length 3)  ;; dr, dg, db; 減衰色
      :alpha :float-le                               ;; 減衰色の不透明度
      :specularity :float-le
      :specular_color (repeated :float-le :length 3) ;; sr, sg, sb; 光沢色
      :mirror_color (repeated :float-le :length 3)   ;; mr, mg, mb; 環境色(ambient)
      :toon_index :ubyte                             ;; toon??.bmp # 0.bmp:0xFF, 1(01).bmp:0x00 ・・・ 10.bmp:0x09
      :edge_flag :ubyte                              ;; 輪郭、影
      :face_vert_count :uint-le                      ;; 面頂点数; この材質で使う面頂点リストのデータ数
      :texture_file_name (string "Windows-31J" :length 20) ;; テクスチャファイル名またはスフィアファイル名
      )
     :prefix :uint-le
     ))))

(def pmd-bone
  ;; ボーンリスト
  (compile-codec
   (ordered-map
    :bone
    (repeated
     (ordered-map
      :bone_name (string "Windows-31J" :length 20)    ;; ボーン名
      :parent_bone_index :ushort-le                   ;; 親ボーン番号
      :tail_pos_bone_index :ushort-le                 ;; tail位置のボーン番号(チェーン末端の場合は2), 親：子は1：多なので、主に位置決め用
      :bone_type :ubyte                               ;; ボーンの種類
      :ik_parent_bone_index :ushort-le                ;; IKボーン番号(影響IKボーン。ない場合は0)
      :bone_head_pos (repeated :float-le :length 3))  ;; x, y, z; ボーンのヘッドの位置
     ;; ボーンリストのサイズ
     :prefix :ushort-le
     ))))

(def pmd-ik
  ;; IKリスト
  (compile-codec
   (ordered-map
    :ik_data
    (repeated
     (ordered-map
      :ik_bone_index :ushort-le
      :ik_target_bone_index :ushort-le
      :ik_chain_bone_index (header
                            (ordered-map
                             :ik_chain_length :ubyte
                             :iterations :ushort-le
                             :control_weight :float-le)
                            ;; bin->fmt
                            (fn [{:keys [ik_chain_length]}]
                              (compile-codec
                               (repeated :ushort-le :length ik_chain_length)))
                            ;; fmt->bin
                            (fn [])
                            :keep-header? true))
     ;; IKリストのサイズ
     :prefix :ushort-le
     ))))

(def pmd-skin
  ;; スキン(表情データ)
  (compile-codec
   (ordered-map
    :skin_data
    (repeated
     (ordered-map
      :skin_name (string "Windows-31J" :length 20)
      :skin_vert_data (header
                       (ordered-map
                        :skin_vert_count :uint-le
                        :skin_type :ubyte)
                       (fn [{:keys [skin_vert_count skin_type]}]
                         (compile-codec
                          (repeated (ordered-map
                                     :skin_vert_index :uint-le
                                     :skin_vert_pos (repeated :float-le :length 3)) :length skin_vert_count)))
                       (fn [])
                       :keep-header? true))
     :prefix :ushort-le))))

(def pmd-skin-disp
  ;; 表情枠用表示リスト
  (compile-codec
   (ordered-map
    :skin_index (repeated :ushort-le :prefix :ubyte))))

(def pmd-bone-disp-names
  ;; ボーン枠用枠名リスト
  (compile-codec
   (ordered-map
    :bone_disp_name (repeated (string "Windows-31J" :length 50) :prefix :ubyte))))

(def pmd-bone-disp
  ;; ボーン枠用表示リスト
  (compile-codec
   (ordered-map
    :bone_disp
    (repeated (ordered-map
               :bone_index :ushort-le
               :bone_disp_frame_index :ubyte)
              :prefix :uint-le))))

(def pmd-english-name-compatibility
  ;; 英名対応
  (compile-codec
   (ordered-map
    :english_name_compatibility :ubyte
    :model_name_eg (string "ISO-8859-1" :length 20)
    :comment_eg (string "ISO-8859-1" :length 256))))

(def pmd-bone-disp-names-en
  ;; ボーンリスト(英語)
  (compile-codec
   (ordered-map
    :bone_name_eg (repeated (string "ISO-8859-1" :length 20) :length 122))))

(def pmd-skin-en
  ;; 表情リスト(英語)
  (compile-codec
   (ordered-map
    :skin_name_eg (repeated (string "ISO-8859-1" :length 20) :length 15))))

(def pmd-bone-disp-fnames-en
  ;; ボーン枠用枠名リスト(英語)
  (compile-codec
   (ordered-map
    :bone_disp_fname_eg (repeated (string "ISO-8859-1" :length 50) :length 7))))

(def pmd-toon-texture-names
  ;; トゥーンテクスチャリスト
  (compile-codec
   (ordered-map
    :toon_file_name (repeated (string "Windows-31J" :length 100) :length 10))))

(def pmd-rigidbody
  ;; 物理演算_剛体リスト
  (compile-codec
   (ordered-map
    :rigidbody
    (repeated (ordered-map
               :rigidbody_name (string "Windows-31J" :length 20) ;; 諸データ：名称 // 頭
               :rigidbody_rel_bone_index :ushort-le              ;; 諸データ：関連ボーン番号 // 03 00 == 3 // 頭
               :rigitbody_group_index :ubyte                     ;; 諸データ：グループ // 00
               :rigidbody_group_target :ushort-le                ;; 諸データ：グループ：対象 // 0xFFFFとの差 // 38 FE
               :shape_type :ubyte                                ;; 形状：タイプ(0:球、1:箱、2:カプセル) // 00 // 球
               :shape_w :float-le                                ;; 形状：半径(幅) // CD CC CC 3F // 1.6
               :shape_h :float-le                                ;; 形状：高さ // CD CC CC 3D // 0.1
               :shape_d :float-le                                ;; 形状：奥行 // CD CC CC 3D // 0.1
               :pos_pos (repeated :float-le :length 3)           ;; 位置：位置(x, y, z)
               :pos_rot (repeated :float-le :length 3)           ;; 位置：回転(rad(x), rad(y), rad(z))
               :rigidbody_weight :float-le                       ;; 諸データ：質量 // 00 00 80 3F // 1.0
               :rigidbody_pos_dim :float-le                      ;; 諸データ：移動減 // 00 00 00 00
               :rigidbody_rot_dim :float-le                      ;; 諸データ：回転減 // 00 00 00 00
               :rigidbody_recoil :float-le                       ;; 諸データ：反発力 // 00 00 00 00
               :rigidbody_friction :float-le                     ;; 諸データ：摩擦力 // 00 00 00 00
               :rigidbody_type :ubyte)                           ;; 諸データ：タイプ(0:Bone追従、1:物理演算、2:物理演算(Bone位置合せ)) // 00 // Bone追従
              :prefix :uint-le))))

(def pmd-joint
  ;; 物理演算_ジョイントリスト
  (compile-codec
   (ordered-map
    :joint
    (repeated (ordered-map
               :joint_name (string "Windows-31J" :length 20)    ;; 諸データ：名称 // 右髪1
               :joint_rigidbody_a :uint-le                      ;; 諸データ：剛体A
               :joint_rigidbody_b :uint-le                      ;; 諸データ：剛体B
               :joint_pos       (repeated :float-le :length 3)  ;; 諸データ：位置(x, y, z) // 諸データ：位置合せでも設定可
               :joint_rot       (repeated :float-le :length 3)  ;; 諸データ：回転(rad(x), rad(y), rad(z))
               :constrain_pos_1 (repeated :float-le :length 3)  ;; 制限：移動1(x, y, z)
               :constrain_pos_2 (repeated :float-le :length 3)  ;; 制限：移動2(x, y, z)
               :constrain_rot_1 (repeated :float-le :length 3)  ;; 制限：回転1(rad(x), rad(y), rad(z))
               :constrain_rot_2 (repeated :float-le :length 3)  ;; 制限：回転2(rad(x), rad(y), rad(z))
               :spring_pos      (repeated :float-le :length 3)  ;; ばね：移動(x, y, z)
               :spring_rot      (repeated :float-le :length 3)) ;; ばね：回転(rad(x), rad(y), rad(z))
              :prefix :uint-le))))
