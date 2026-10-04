module operators {
  pure def add(a, b) = a + b
  pure def sub(a, b) = a - b

  action increment(s) = s' = s + 1

  val result = add(1, 2)

  val cond = if (result > 0) true else false

  val conjunction = and {
    result > 0,
    result < 10,
  }

  val disjunction = or {
    result == 0,
    result == 3,
  }
}
