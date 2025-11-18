import threading
from datetime import datetime
import subprocess
import os
import sys

import extractADBEvent

def simple_touch_recorder():
    subprocess.call("@echo off | title Motion Record (by ADB)| chcp 65001 | cls", shell=True)
    # 获取设备列表
    result = subprocess.run("adb devices", shell=True, capture_output=True, text=True)
    lines = result.stdout.strip().split('\n')[1:]  # 分割，跳过标题行

    devices = []
    for line in lines:
        if line.strip():
            parts = line.split('\t')
            if len(parts) >= 2:
                devices.append(parts[0])

    if not devices:
        print("未找到设备")
        return

    # 选择设备
    if len(devices) > 1:
        print("可用设备:")
        for i, device in enumerate(devices):
            print(f"\t{i + 1}. {device}")

        choice = int(input("选择设备: ")) - 1
        device_id = devices[choice]
    else:
        device_id = devices[0]

    # 查找触摸设备
    result = subprocess.run(f"adb -s {device_id} shell getevent -lp", shell=True, capture_output=True, text=True)

    touch_device = None
    current_device = None

    for line in result.stdout.split('\n'):
        if "add device" in line:
            parts = line.split()
            if len(parts) >= 4:
                current_device = parts[3]
        elif "ABS_MT_TOUCH_MAJOR" in line and current_device:
            touch_device = current_device
            break

    if not touch_device:
        print("未找到触摸设备")
        return
    print(f"找到触摸设备: {device_id}; {touch_device}")

    # 选择应用
    subprocess.call(f"cls | title Motion Record (by ADB) - 触摸设备: {device_id}; {touch_device}", shell=True)
    apps = {
        "原神": "com.miHoYo.Yuanshen/com.miHoYo.GetMobileInfo.MainActivity",
    }
    print("请选择你需要打开的应用：\n"
          + '\n'.join(f"\t{i}. {app}" for i, app in enumerate(apps, 1)))

    choice = int(input("选择应用: ")) - 1

    # 启动应用
    subprocess.run(
        f'adb -s {device_id} shell am start -n "{tuple(apps.values())[choice]}',
        shell=True)

    input("应用已启动，按回车开始记录触摸事件...")

    # 记录事件
    process = subprocess.Popen(f"adb -s {device_id} shell getevent -lt {touch_device}",
                               shell=True, stdout=subprocess.PIPE, text=True)
    # for l in process.stdout:
    #     e = extractADBEvent.separate(l)
    #     s = e.__str__()
    #     if type(e.value) is int:
    #         s += f",{e.value/16}"
    #     print(s)

    extractADBEvent.isRunnable = True
    th = threading.Thread(target=extractADBEvent.extractADBEvent, args=(process.stdout, ))
    th.start()

    input("开始记录... 输入任意键终止录制...")
    process.terminate()
    extractADBEvent.isRunnable = False

    deposit = extractADBEvent.depositPrimaryFingers(extractADBEvent.fingers)
    my_csv = extractADBEvent.getCSV(deposit)

    with open(f"./recordings/converted-{getDate()}.csv", "w") as f:
        f.write(my_csv)

    print("\n记录已保存")
    subprocess.call(f"pause", shell=True)

# def translateGetevent(out):
#     time =
#     for line in out:


def getDate():
    date = datetime.now()
    return "%d%d%d-%d%d%d"%(date.year, date.month, date.day, date.hour, date.minute, date.second)

if __name__ == "__main__":
    # print(test())
    simple_touch_recorder()