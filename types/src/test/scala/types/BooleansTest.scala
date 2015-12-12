package types

import org.scalatest.FunSuite

class BooleansTest extends FunSuite {
  import Booleans._
  test("booleans"){
    assert(valueOfType[True])
    assert(!valueOfType[False])
  }
}
