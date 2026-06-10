package caliniya.armavoke.system.input;

import arc.input.GestureDetector.GestureListener;
import arc.input.KeyCode;
import arc.math.Mathf;
import caliniya.armavoke.world.stars.Universe;

public class UniverseInput implements GestureListener {
  
  @Override
  public boolean tap(float X, float Y, int count, KeyCode key) {
    if(key == KeyCode.mouseLeft) {
    	Universe.chooseX = (int)X;
      Universe.chooseY = (int)Y;
      return true;
    }
    return false;
  }
  
  
}
