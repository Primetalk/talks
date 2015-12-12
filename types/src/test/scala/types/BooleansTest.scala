package types

import org.scalatest.FunSuite

class BooleansTest extends FunSuite {
  import Booleans._
  test("booleans"){
    assert(booleanOfType[True])
    assert(!booleanOfType[False])
  }
}
