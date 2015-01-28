package com.heeresonline.gameloop;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LightingColorFilter;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.PaintDrawable;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class GameSurfaceView extends SurfaceView implements Runnable {
  private boolean isRunning = false;
  private Thread gameThread;
  private SurfaceHolder holder;

  private int screenWidth;
  private int screenHeight;

  private Sprite[] sprites;

  private final static int MAX_FPS = 40; //desired fps
  private final static int FRAME_PERIOD = 1000 / MAX_FPS; // the frame period

  public GameSurfaceView(Context context) {
    super(context);

    holder = getHolder();
    holder.addCallback(new SurfaceHolder.Callback() {
      @Override
      public void surfaceCreated(SurfaceHolder holder) {
      }

      @Override
      public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        screenWidth = width;
        screenHeight = height;
      }

      @Override
      public void surfaceDestroyed(SurfaceHolder holder) {
      }
    });

    sprites = new Sprite[] {
      new Sprite(100, 100, BitmapFactory.decodeResource(this.getResources(), R.drawable.ic_launcher)),
      new Sprite(600, 400, BitmapFactory.decodeResource(this.getResources(), R.drawable.ic_launcher), Color.RED),
      new Sprite(400, 800, BitmapFactory.decodeResource(this.getResources(), R.drawable.ic_launcher), Color.BLUE)
	};
  }

  /**
   * Start or resume the game.
   */
  public void resume() {
    isRunning = true;
    gameThread = new Thread(this);
    gameThread.start();
  }

  /**
   * Pause the game loop
   */
  public void pause() {
    isRunning = false;
    boolean retry = true;
    while (retry) {
      try {
        gameThread.join();
        retry = false;
      } catch (InterruptedException e) {
        // try again shutting down the thread
      }
    }
  }

  class Sprite {
    int x;
    int y;
    int directionX = 1;
    int directionY = 1;
    int speed = 10;
    int color = 0;
    Bitmap image;

    public Sprite(int x, int y) {
      this.x = x;
      this.y = y;
    }

    public Sprite(int x, int y, Bitmap image) {
      this(x, y);
      this.image = image;
    }

    public Sprite(int x, int y, Bitmap image, int color) {
      this(x, y, image);
      this.color = color;
    }
  }

  protected void step() {
    for (int index = 0, length = sprites.length; index < length; index++) {
      Sprite sprite = sprites[index];

      if ((sprite.x < 0) || ((sprite.x + sprite.image.getWidth()) > screenWidth)) {
        sprite.directionX *= -1;
      }
      if ((sprite.y < 0) || ((sprite.y + sprite.image.getHeight()) > screenHeight)) {
        sprite.directionY *= -1;
      }

      Rect current = new Rect(sprite.x, sprite.y,
                              sprite.x + sprite.image.getWidth(),
                              sprite.y + sprite.image.getHeight());
      for (int subindex = 0; subindex < length; subindex++) {
        if (subindex != index) {
          Sprite subsprite = sprites[subindex];
          Rect other = new Rect(subsprite.x, subsprite.y,
                                subsprite.x + subsprite.image.getWidth(),
                                subsprite.y + subsprite.image.getHeight());
          if (Rect.intersects(current, other)) {
            // Poor physics implementation.
            sprite.directionX *= -1;
            sprite.directionY *= -1;
          }
        }
      }

      sprite.x += (sprite.directionX * sprite.speed);
      sprite.y += (sprite.directionY * sprite.speed);
    }
  }

  protected void render(Canvas canvas) {
    canvas.drawColor(Color.BLACK);
    for (int index = 0, length = sprites.length; index < length; index++) {
      Paint p = null;
      if (sprites[index].color != 0) {
        p = new Paint();
        ColorFilter filter = new LightingColorFilter(sprites[index].color, 0);
        p.setColorFilter(filter);
      }

      canvas.drawBitmap(sprites[index].image, sprites[index].x, sprites[index].y, p);
    }
  }

  @Override
  public void run() {
    while(isRunning) {
      // We need to make sure that the surface is ready
      if (! holder.getSurface().isValid()) {
        continue;
      }
      long started = System.currentTimeMillis();

      // update
      step();
      // draw
      Canvas canvas = holder.lockCanvas();
      if (canvas != null) {
        render(canvas);
        holder.unlockCanvasAndPost(canvas);
      }

      float deltaTime = (System.currentTimeMillis() - started);
      int sleepTime = (int) (FRAME_PERIOD - deltaTime);
      if (sleepTime > 0) {
        try {
          gameThread.sleep(sleepTime);
        }
        catch (InterruptedException e) {
        }
      }
      while (sleepTime < 0) {
        step();
        sleepTime += FRAME_PERIOD;
      }
    }
  }
}
