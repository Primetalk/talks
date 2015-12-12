package types

import scala.language.higherKinds

/**
  * Created by zhizhelev on 12.12.15.
  */
object Booleans {
  sealed trait Bool {
    type Branch[T,F]
  }
  sealed trait True extends Bool{
    type Branch[T,F] = T
  }
  sealed trait False extends Bool{
    type Branch[T,F] = F
  }

  type &&[A<:Bool, B<:Bool] = A#Branch[B,False]
  type ||[A<:Bool, B<:Bool] = A#Branch[True,B]

//  case class TypeConverter[A,B](value:B)
//  implicit def booleanOfType[A](implicit tc:TypeConverter[A,Boolean]) = tc.value
//  implicit val trueConverter = TypeConverter[True,Boolean](true)
//  implicit val falseConverter = TypeConverter[False,Boolean](false)

  case class Service[Started<:Bool](name:String, resources:Any)
  def start(service:Service[False]) = service.copy(resources = Some('resource)).asInstanceOf[Service[True]]
  def stop(service:Service[True]) = service.copy(resources = None).asInstanceOf[Service[False]]

  class S[T](implicit evidence:T =:= Int) {
//    require(implicitly[
  }

  /// Type-Lambdas
  type _1[F[_,_],A] = {type λ[Z] = F[Z,A]}
  type _2[F[_,_],A] = {type λ[Z] = F[A,Z]}

  case class TypeConverter[A,B](value:B){ type Value = B; def _value:Value = value}
  implicit def valueOfType[A](implicit tc:TypeConverter[A,_]):tc.Value = tc._value

  implicit val trueConverter = TypeConverter[True,Boolean](true)
  implicit val falseConverter = TypeConverter[False,Boolean](false)

  val _true = valueOfType[True]

  require(_true)

}
