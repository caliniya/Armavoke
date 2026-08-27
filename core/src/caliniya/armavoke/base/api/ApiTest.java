package caliniya.armavoke.base.api;

import caliniya.armavoke.annotation.Annotations.*;
import caliniya.armavoke.base.anno.auto.AnnoProc;

@SystemDef(name = "aaa" , thread = "aaa" , index =5)
@Component(name = "test")
@Entity(name = "test" , comps = {ApiTest.class})
public class ApiTest {
public int y;
}