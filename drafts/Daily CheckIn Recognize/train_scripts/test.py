import cv2
import torch
from matplotlib import pyplot as plt
from ultralytics import YOLO

model = YOLO('./trained-weights/640 8b 0.02 --+ 0.004.pt')


def test():
    # 分别在训练集和验证集上测试
    train_images = "./datas/mys/images/train/"
    val_images = "./datas/mys/images/val/"
    # neg_images = "./datas/mys/NegativeImages/"

    print("=== 训练集检测 ===")
    train_results = list(model(source=train_images, conf=0.3, save=False))
    train_detections = sum(len(r.boxes) for r in train_results)
    print(f"训练集检测到 {train_detections} 个目标")

    print("\n=== 验证集检测 ===")
    val_results = list(model(source=val_images, conf=0.3, save=False))
    val_detections = sum(len(r.boxes) for r in val_results)
    print(f"验证集检测到 {val_detections} 个目标")

    # print("\n=== 负性集检测 ===")
    # val_results = list(model(source=neg_images, conf=0.3, save=False))
    # val_detections = sum(len(r.boxes) for r in val_results)
    # print(f"负性集检测到 {val_detections} 个目标")

    print(f"\n过拟合比率: {train_detections / max(val_detections, 1):.1f}x")


def detect():
    print("read img:")

    # 读取图片
    img = cv2.imread('./datas/mys/images/train/GS1.jpg')
    thing = model(img)

    # 推理
    print("\n\n\n\nresult:")
    results = thing[0]
    print(results)
    print("\n\n\n\nboxes:")
    for box in results.boxes:
        print(box)

    # 绘制结果
    annotated_img = results.plot()  # 返回带标注的BGR图片

    # 缩小显示
    scale_percent = 50  # 缩小50%
    width = int(annotated_img.shape[1] * scale_percent / 100)
    height = int(annotated_img.shape[0] * scale_percent / 100)
    resized = cv2.resize(annotated_img, (width, height))

    # 显示图片
    cv2.imshow('YOLO Detection', resized)
    cv2.waitKey(0)
    cv2.destroyAllWindows()

    # 使用matplotlib显示（推荐）
    plt.figure(figsize=(12, 8))  # 控制显示大小
    # 转换为RGB显示
    plt.imshow(cv2.cvtColor(annotated_img, cv2.COLOR_BGR2RGB))
    plt.axis('off')
    plt.title('YOLO Detection Results')
    plt.show()

    # 保存结果
    cv2.imwrite('output.jpg', annotated_img)


def exportNCNN():
    model.export(format="ncnn")


def testYoloModel():
    model.eval()

    # 创建测试输入
    dummy_input = torch.randn(1, 3, 640, 640) / 255.0  # 归一化

    # 推理
    with torch.no_grad():
        outputs = model(dummy_input)

    # 详细分析输出结构
    print("输出类型:", type(outputs))
    print("输出长度:", len(outputs))

    # 分析第一个输出（通常是检测结果）
    if len(outputs) > 0:
        output = outputs[0]
        print("\n第一个输出类型:", type(output))
        print("是否是Results对象:", hasattr(output, 'boxes'))

        # 如果是Ultralytics的Results对象
        if hasattr(output, 'boxes'):
            print("\nResults对象属性:")
            print("- boxes属性类型:", type(output.boxes))
            print("- boxes存在:", output.boxes is not None)
            print("- boxes数据形状:", output.boxes.data.shape if output.boxes is not None else None)

            # 查看box的格式
            if output.boxes is not None:
                print("\n检测框信息:")
                print("- 检测框数量:", len(output.boxes))
                print("- 检测框数据类型:", output.boxes.data.dtype)
                print("- 检测框形状:", output.boxes.data.shape)

                # 查看具体内容
                print("\n前3个检测框内容:")
                for i in range(min(3, len(output.boxes))):
                    box = output.boxes.data[i]
                    print(f"  框{i}: xyxy={box[:4].cpu().numpy()}, 置信度={box[4]:.4f}, 类别={box[5].int()}")

        # 如果是Tensor或其他类型
        elif isinstance(output, torch.Tensor):
            print("输出是Tensor:")
            print("形状:", output.shape)
            print("前几个值:", output[0, :5])
        elif isinstance(output, list):
            print("输出是List:")
            for i, item in enumerate(output):
                print(f"  第{i}项类型: {type(item)}, 形状: {item.shape if hasattr(item, 'shape') else '无形状'}")

if __name__ == '__main__':
    # model = YOLO("./runs/detect/mys2/weights/best.pt")
    detect()