package caliniya.vergvoke.base.api;

import caliniya.vergvoke.annotation.Annotations.*;
import caliniya.vergvoke.base.anno.auto.AnnoProc;

@SystemDef(name = "aaa" , thread = "aaa" , index =5)
@Component(name = "test")
@Entity(name = "test" , comps = {ApiTest.class})
public class ApiTest {
public int y;
}