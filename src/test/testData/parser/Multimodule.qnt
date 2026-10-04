module A {
  const N : int
  var counter : int

  val init = counter' = 0
  def step = counter' = counter + 1

  val inv = counter >= 0
}

module B {
  import A

  val check = A::inv
}
