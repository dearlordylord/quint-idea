module types {
  type Alias = int
  type Pair = (int, str)
  type Record = { name: str, age: int }

  type Color =
    | Red
    | Green
    | Blue(int)

  pure def identity(x) = x
}
