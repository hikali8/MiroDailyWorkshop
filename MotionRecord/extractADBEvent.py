import re
from datetime import datetime
from enum import Enum


# purify off and leave only the coordinate information
fingers = None
isRunnable = False
def extractADBEvent(out):
    global fingers
    finger = Finger()
    fingers = Fingers(finger)

    for line in out:
        if (not isRunnable):
            break
        event = separate(line)
        if not finger:
            finger.appendAction(event.time)
            fingers.startTime = event.time
        elif finger.last().time < event.time:
            finger.concludeLast()

        match event.code:
            case "ABS_MT_POSITION_X":
                if finger.last().time < event.time:
                    finger.appendAction(event.time)
                finger.last().x = event.value
            case "ABS_MT_POSITION_Y":
                if finger.last().time < event.time:
                    finger.appendAction(event.time)
                finger.last().y = event.value
            case "ABS_MT_SLOT":
                finger.concludeLast()
                slot = event.value
                fingers.completeLength(slot)
                finger = fingers[slot]
                finger.appendAction(event.time)
            case "ABS_MT_TRACKING_ID":
                if finger.last().time < event.time:
                    finger.appendAction(event.time)
                if event.value == 0xffffffff:
                    finger.last().type = Action.Type.Up
                else:
                    finger.last().type = Action.Type.Down
    return fingers



class Fingers(list):
    def __init__(self, finger=None):
        super().__init__()
        self.startTime = None
        if finger is not None:
            self.append(finger)

    def completeLength(self, target: int):
        for i in range(target + 1 - len(self)):
            self.append(Finger())

class Finger(list):
    def last(self):
        return self[-1]

    def appendAction(self, time):
        self.append(Action(time))

    def concludeLast(self):
        last = self.last()
        if last.type is not None:
            return
        if last.x is None and last.y is None:
            self.pop()
            return
        last.type = Action.Type.Move
        pass


class Action:
    class Type(Enum):
        Down = 1
        Move = 2
        Up = 3
        wait = 4
        NEXT = 5

    def __init__(self, time=None):
        self.type = None
        self.x = None
        self.y = None
        self.time = time

    def __str__(self):
        return f"%s, %s, %s, %s"%(self.time, self.type, self.x, self.y)

class Event:
    def __init__(self, time: str, type: str, code: str, value: str):
        self.time = self.getMicroSec(time)
        self.type = type
        self.code = code
        try:
            self.value = int(value, 16)
        except:
            self.value = value

    @staticmethod
    def getMicroSec(time: str):
        pos = time.find('.')
        return int(time[:pos] + time[pos + 1:])

    def __str__(self):
        return ",".join((self.time.__str__(), self.type, self.code, self.value.__str__()))

def separate(line: str):
    line = line.strip()
    groups = re.match(r"^\[\s*([\d\.]*)\]\s*(\S*)\s*(\S*)\s*(\S*)$", line)
    return Event(groups[1], groups[2], groups[3], groups[4])


def depositPrimaryFingers(fingers: Fingers, isToMergeMoves=False):
    startTime = fingers.startTime
    currentTime = startTime

    new_fingers = Fingers()
    for finger in fingers:
        new_finger = Finger()
        new_fingers.append(new_finger)
        for action in finger:
            if len(new_fingers) == 1 and not new_finger:
                _action = Action()
                _action.type = Action.Type.Down
                _action.x = action.x
                _action.y = action.y
                new_finger.append(_action)
                continue
            if not new_finger:
                currentTime = action.time
                _action = Action(action.time - startTime)
                _action.type = Action.Type.NEXT
                new_finger.append(_action)

            last = new_finger.last()
            if (isToMergeMoves and
                    action.type == Action.Type.Move and
                    last.type == Action.Type.Move and
                    action.time - currentTime < 500000):
                # 相近Move合并


                if action.x is not None:
                    last.x = action.x
                if action.y is not None:
                    last.y = action.y
                last.time += action.time - currentTime
                currentTime = action.time
                continue

            delta = action.time - currentTime
            if delta > 0:
                _action = Action(delta)
                _action.type = Action.Type.wait
                new_finger.append(_action)
                currentTime = action.time
                last = _action

            match action.type:
                case Action.Type.Move:
                    _action = Action(0)
                    if last.type == Action.Type.wait:
                        if last.time > 8000:
                            _action.time = 8000
                            last.time -= 8000
                            if last.time < 500:
                                new_finger.pop()
                        else:
                            _action.time = last.time
                            new_finger.pop()
                    _action.type = Action.Type.Move
                    _action.x = action.x
                    _action.y = action.y
                    new_finger.append(_action)
                case Action.Type.Down:
                    _action = Action()
                    _action.type = action.type
                    _action.x = action.x
                    _action.y = action.y
                    new_finger.append(_action)
                case Action.Type.Up:
                    _action = Action()
                    _action.type = action.type
                    new_finger.append(_action)
                case _:
                    raise Exception("Unknown type from extraction.")
    return new_fingers



def getCSV(fingers):
    ret = ""
    for finger in fingers:
        for action in finger:
            ret += action.type.name + ","
            if action.time is not None:
                ret += str(round(action.time / 1000))
            ret += ","
            if action.x is not None:
                ret += str(action.x / 16)
            ret += ","
            if action.y is not None:
                ret += str(action.y / 16)
            ret = ret.rstrip(',')
            ret += '\n'
    return ret


def getDate():
    date = datetime.now()
    return "%d%d%d-%d%d%d"%(date.year, date.month, date.day, date.hour, date.minute, date.second)

if __name__ == '__main__':
    # ret = separate(" [ 3722633.594610] EV_ABS       ABS_MT_TRACKING_ID   ffffffff    \n")
    # print(ret)
    with open(f"./recordings/converted-{getDate()}.txt", "r") as f:
        fingers = extractADBEvent(f)
    # for f in fingers:
    #     print("=======finger=======")
    #     for a in f:
    #         print(a)
    print("-"*40)
    deposit = depositPrimaryFingers(fingers)
    print(getCSV(deposit))
