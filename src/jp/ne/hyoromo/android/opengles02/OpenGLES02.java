package jp.ne.hyoromo.android.opengles02;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLSurfaceView;
import android.opengl.GLU;
import android.opengl.GLUtils;
import android.os.Bundle;
import android.os.SystemClock;

/**
 * 正方形の板にテクスチャを貼り付けたオブジェクトが縦に回転し続ける
 * 
 * @author hyoromo
 */
public class OpenGLES02 extends Activity {
    private GLSurfaceView mView;
    private MyRenderer mRenderer;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mView = new GLSurfaceView(this);
        mRenderer = new MyRenderer();
        mView.setRenderer(mRenderer);
        setContentView(mView);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mView.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mView.onResume();
    }

    private class MyRenderer implements GLSurfaceView.Renderer {
        private FloatBuffer mQuadBuff; // 頂点正方形バッファ
        private FloatBuffer mTexBuff; // 頂点テクスチャバッファ
        private int mTextureNo; // テクスチャ管理番号

        // ライティングの定義
        float[] lightAmbient = new float[] { 0.2f, 0.2f, 0.2f, 1.0f };// 光源アンビエント
        float[] lightDiffuse = new float[] { 1, 1, 1, 1 }; // 光源ディフューズ
        float[] lightPos = new float[] { 1, 1, 1, 1 }; // 光源位置
        // 正方形がなにでできているかを定義
        float[] matAmbient = new float[] { 1f, 1f, 1f, 1.0f };// マテリアルアンビエント
        float[] matDiffuse = new float[] { 1f, 1f, 1f, 1.0f };// マテリアルディフューズ

        public MyRenderer() {
            // 正方形の頂点設定(RGB * (左上/右上/左下/右下))
            float[] vertices = {
                    -0.5f, -0.5f, 0.0f,
                     0.5f, -0.5f, 0.0f,
                     -0.5f, 0.5f, 0.0f,
                      0.5f, 0.5f, 0.0f
            };

            // テクスチャの頂点設定
            float[] texCoords = {
                    0.0f, 1.0f,
                    1.0f, 1.0f,
                    0.0f, 0.0f,
                    1.0f, 0.0f
            };

            // 正方形
            ByteBuffer byteBuffer = ByteBuffer.allocateDirect(vertices.length * 4);
            byteBuffer.order(ByteOrder.nativeOrder());
            mQuadBuff = byteBuffer.asFloatBuffer();
            mQuadBuff.put(vertices);
            mQuadBuff.position(0);

            // テクスチャ
            byteBuffer = ByteBuffer.allocateDirect(texCoords.length * 4);
            byteBuffer.order(ByteOrder.nativeOrder());
            mTexBuff = byteBuffer.asFloatBuffer();
            mTexBuff.put(texCoords);
            mTexBuff.position(0);
        }

        /**
         * GLSurfaceViewが生成されたときに呼ばれる。
         */
        public void onSurfaceCreated(GL10 gl, EGLConfig config) {
            // 背景を黒に設定
            gl.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
            gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);

            gl.glMatrixMode(GL10.GL_PROJECTION);
            gl.glLoadIdentity();
            // 正方形の座標定義
            GLU.gluOrtho2D(gl, -1.0f, 1.0f, -1.0f, 1.0f);

            // ビットマップ読込
            // テクスチャの名前を取得
            Bitmap bmp = BitmapFactory.decodeResource(getResources(), R.drawable.icon);
            int[] textureNo = new int[1];
            // テクスチャオブジェクトの作成
            gl.glGenTextures(1, textureNo, 0);
            mTextureNo = textureNo[0];

            // テクスチャをロードする
            // 第1引数では2Dであることを指定している。第2引数に対応したテクスチャオブジェクトを有効化する。
            // バインドされたオブジェクトに対して、2D画像を割り当てる。
            gl.glBindTexture(GL10.GL_TEXTURE_2D, mTextureNo);

            // GL用のメモリにバインドされたビットマップデータを受け渡す。
            GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, bmp, 0);

            // テクスチャ有効
            gl.glEnable(GL10.GL_TEXTURE_2D);

            // テクスチャマッピングの調整(これないと上手くテクスチャが張り付かない)
            gl.glTexParameterx(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_LINEAR);

            // 光源(これないとオブジェクトとテクスチャが混ざる)
            gl.glEnable(GL10.GL_LIGHTING);
            gl.glEnable(GL10.GL_LIGHT0);
            gl.glLightfv(GL10.GL_LIGHT0, GL10.GL_AMBIENT, lightAmbient, 0);
            gl.glLightfv(GL10.GL_LIGHT0, GL10.GL_DIFFUSE, lightDiffuse, 0);
            gl.glLightfv(GL10.GL_LIGHT0, GL10.GL_POSITION, lightPos, 0);

            // マテリアル定義(これないと光源の当たらない箇所が描画されない)
            gl.glMaterialfv(GL10.GL_FRONT_AND_BACK, GL10.GL_AMBIENT, matAmbient, 0);
            gl.glMaterialfv(GL10.GL_FRONT_AND_BACK, GL10.GL_DIFFUSE, matDiffuse, 0);
        }

        /**
         * GLSurfaceViewのサイズが変更されたときに呼ばれる。 端末を傾けたときに縦長/横長に切り替わるときに呼ばれるメソッド。
         */
        public void onSurfaceChanged(GL10 gl, int width, int height) {
            if (width > height) {
                // ランドスケープ(横長)
                gl.glViewport((width - height) / 2, 0, height, height);
            } else {
                // ポートレート(縦長)
                gl.glViewport(0, (height - width) / 2, width, width);
            }
        }

        /**
         * 画面を描画するときに呼ばれる。 経過時間で正方形を回転させている。
         */
        public void onDrawFrame(GL10 gl) {
            // 画面クリア
            gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);

            // FPS制御？ないとFPSが不安定になる。
            gl.glLoadIdentity();

            // 3.6秒で1回転(面白いから実装)
            gl.glRotatef((SystemClock.uptimeMillis() % 3600) * 0.1f, 1.0f, 0, 0);

            // 正方形の色を変更(背景色なのでテクスチャに隠れて見えない)
            gl.glColor4f(1.0f, 0.0f, 0.0f, 1.0f);

            // 正方形に画像貼り付け(バインド)
            gl.glBindTexture(GL10.GL_TEXTURE_2D, mTextureNo);

            // 頂点バッファ機能ON
            gl.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY);

            // 頂点バッファの設定
            gl.glVertexPointer(3, GL10.GL_FLOAT, 0, mQuadBuff);
            gl.glTexCoordPointer(2, GL10.GL_FLOAT, 0, mTexBuff);

            // 描画
            gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 0, 4);
        }
    }
}