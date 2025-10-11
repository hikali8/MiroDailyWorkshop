from ultralytics import YOLO

'''
Procedure:
 1. bs = 64 lr = 0.2 -> 0.02
 2. bs = 16 lr = 0.2 -> 0.02
 3. bs = 8  lr = 0.02 -> 0.002
'''

if __name__ == '__main__':
    model = YOLO('./pretrained-weights/640 16b 0.2 --+ 0.02.pt')

    results = model.train(data="./datas/IconLabeling/data.yaml",
                        imgsz=640,
                        workers=3,
                        device=0,

                        epochs=200,
                        batch=8,
                        lr0=0.2,
                        lrf=0.1,
                        dropout=0.1,
                        weight_decay=0.0005,
                        warmup_epochs=3,

                        save=True,
                        name="IconLabeling",

                        translate=0.3,  # 平移
                        scale=0.5,      # 缩放

                        # unnecessary params
                        hsv_h=0,        # 色调变化
                        hsv_s=0,        # 饱和度变化
                        hsv_v=0,        # 明度变化
                        degrees=0,      # 旋转角度
                        shear=0,        # 剪切变换
                        patience=0      # stop if no change in [patience] steps
                        )