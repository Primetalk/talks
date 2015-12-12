package types

/**
  * Created by zhizhelev on 12.12.15.
  */
object Gadt {
 /*
  abstract class Exp[A] {
    def eval = this match {
      case LitInt(i)       => i
      case LitBool(b)      => b
      case Add(e1, e2)     => e1.eval + e2.eval
      case Mul(e1, e2)     => e1.eval * e2.eval
      case Cond(b,thn,els) => if ( b.eval ) { thn.eval } else { els.eval }
      case Eq(e1,e2)       => e1.eval == e2.eval
    }
  }
*/
 object Exp {
   def evalAny[A](e: Exp[A]): A = e match {
     case LitInt(i)         => i
     case LitBool(b)        => b
     case Add(e1, e2)       => e1.eval + e2.eval
     case Mul(e1, e2)       => e1.eval * e2.eval
     case Cond(b, thn, els) => if (b.eval) { thn.eval } else { els.eval }
     case Eq(e1, e2)        => e1.eval == e2.eval
   }
 }

  abstract class Exp[A] {
    def eval: A = Exp.evalAny(this)
  }

  case class LitInt(i: Int)                                     extends Exp[Int]
  case class LitBool(b: Boolean)                                extends Exp[Boolean]
  case class Add(e1: Exp[Int], e2: Exp[Int])                    extends Exp[Int]
  case class Mul(e1: Exp[Int], e2: Exp[Int])                    extends Exp[Int]
  case class Cond[A](b: Exp[Boolean], thn: Exp[A], els: Exp[A]) extends Exp[A]
  case class Eq[A](e1: Exp[A], e2: Exp[A])                      extends Exp[Boolean]
}
