import math

import ncnn
import cv2
import numpy as np
import os
from datetime import datetime

# 1. open model
# 2. image normalize
# 3. infer and recognize
# 4. draw out the image

ClassNames = ['GS_CheckIn',
              'HK2_CheckIn',
              'HK3_CheckIn',
              'HSR_CheckIn',
              'ZZZ_CheckIn']
target_size = 640
confidence = 0.5
normalization = [1 / 255, 1 / 255, 1 / 255]

net = ncnn.Net()
net.load_param("./trained-weights/model.ncnn.param")
net.load_model("./trained-weights/model.ncnn.bin")
img = cv2.imread("./datas/mys/images/train/HSR1.jpg")
if img is None: raise ValueError("Can't read image.")

scale = None
wpadP2 = None
hpadP2 = None
def image_normalization() -> ncnn.Mat:
    global scale, wpadP2, hpadP2
    w = img.shape[1]
    h = img.shape[0]
    if w > h:
        scale = float(w) / target_size
        h = int(h / scale)
        w = target_size
    else:
        scale = float(h) / target_size
        w = int(w / scale)
        h = target_size
    in_mat = ncnn.Mat.from_pixels_resize(img,
                                         ncnn.Mat.PixelType.PIXEL_BGR2RGB,
                                         img.shape[1],
                                         img.shape[0],
                                         w,
                                         h
                                         )
    # turn the w and h into a multiplier of 32
    # wpadP2 = (math.ceil(w / 32) * 32 - w) / 2
    # hpadP2 = (math.ceil(h / 32) * 32 - h) / 2
    # turn to 640x640
    wpadP2 = (target_size - w) / 2
    hpadP2 = (target_size - h) / 2
    in_mat = ncnn.copy_make_border(
        in_mat,
        math.floor(hpadP2),
        math.ceil(hpadP2),
        math.floor(wpadP2),
        math.ceil(wpadP2),
        ncnn.BorderType.BORDER_CONSTANT,
        114.0,
    )
    in_mat.substract_mean_normalize([], normalization)
    print(wpadP2, hpadP2)
    return in_mat


in_mat = image_normalization()

class Object:
    def __init__(self, confidence=0, classIndex=-1, location=()):
        self.confidence = confidence
        self.classIndex = classIndex
        self.location = location

    def __repr__(self):
        return f"obj {ClassNames[self.classIndex]}: confidence {self.confidence}, location {self.location}, ori {self.ori_loc}\n"


objList = []
def infer_and_recognize():
    print("=====infer_and_recognize=====")
    extractor = net.create_extractor()
    extractor.input("in0", in_mat)
    _, out_mat = extractor.extract("out0")
    assert in_mat.w == target_size and in_mat.h == target_size

    print(in_mat.w, in_mat.h, in_mat.c)
    print(out_mat.w, out_mat.h, out_mat.c)

    # 8400个值：代表8400个框。9个值：代表5种对象的得分，和4个表示框位置的值。
    out_mat = np.array(out_mat).transpose()
    for box in out_mat:
        obj = Object()
        for j in range(4, 9):
            class_score = box[j]
            if class_score > confidence and class_score > obj.confidence:
                obj.confidence = class_score
                obj.classIndex = j - 4
                obj.location = box[:4]
        if obj.confidence <= 0:
            continue
        for obj_past in objList:
            if obj_past.classIndex != obj.classIndex:
                continue
            for v1, v in zip(obj_past.location, obj.location):
                if abs(v1 / v - 1) > 0.3:
                    break
            else:
                if obj_past.confidence < obj.confidence:
                    obj_past.confidence = obj.confidence
                    obj_past.location = obj.location
                break
        else:
            objList.append(obj)

infer_and_recognize()


def transform(x):
    return round(scale * x)

def corrrect_coordinate():
    # yolo模型原输出是 centerX, centerY, width, height
    for obj in objList:
        centerX, centerY, width, height = obj.location
        realCenterX = centerX - wpadP2
        realCenterY = centerY - hpadP2
        x1 = realCenterX - width / 2
        x2 = realCenterX + width / 2
        y1 = realCenterY - height / 2
        y2 = realCenterY + height / 2
        obj.ori_loc = obj.location
        obj.location = (transform(x1), transform(y1), transform(x2), transform(y2))

corrrect_coordinate()
print(objList)

def draw_picture():
    _img = img.copy()
    for obj in objList:
        x1, y1, x2, y2 = obj.location
        label = f"{ClassNames[obj.classIndex]}: {obj.confidence:.3f}"
        cv2.rectangle(_img, (x1, y1), (x2, y2), (0, 255, 0), 3)

        # 标签
        (tw, th), _ = cv2.getTextSize(label, cv2.FONT_HERSHEY_SIMPLEX, 1, 2)
        cv2.rectangle(_img, (x1, y1 - th - 10), (x1 + tw, y1), (0, 255, 0), -1)
        cv2.putText(_img, label, (x1, y1 - 5),
                    cv2.FONT_HERSHEY_SIMPLEX, 1, (255, 255, 255), 2)
    # 显示
    display_scale = min(1200 / _img.shape[1], 800 / _img.shape[0])
    display_w = int(_img.shape[1] * display_scale)
    display_h = int(_img.shape[0] * display_scale)
    display_img = cv2.resize(_img, (display_w, display_h))

    cv2.imshow("Detected (Green=Detected)", display_img)
    cv2.waitKey(0)
    cv2.destroyAllWindows()

draw_picture()