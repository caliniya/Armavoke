package caliniya.armavoke.base.type;

import caliniya.armavoke.game.data.*;
import caliniya.armavoke.core.*;
import caliniya.armavoke.game.*;

public enum TeamTypes {
  Evoke,
  Veto,
  Abort,
  Mutex;

  /** 快捷获取该团队的运行时数据 */
  public TeamData data() {
    return Teams.get(this);
  }
}
