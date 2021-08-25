## Haskell由来

美国数学家ALonzo Church 引入lambda calculus ，通过使用符号表达变量的帮顶和替换来定义函数计算的系统。

## lambda|定义

描述一个形式系统需要约定用到的基本符号，

## 递归

扩展递归和尾递归

```haskell
factorial n=if n==0 then 1 else n * factorial(n-1)
```

`factorial`在没有结束的时候，会不停扩展，导致所有中间结果在内存，这种方式被称`扩展递归`

尾递归

```haskell
mygcd x y = if y==0 then x else mygcd y (mod x y)
```

`mygcd`没有中间缓存变量，不必展开，这种递归被称为`尾递归`

### beta归约

> ((lambda V.E)E')=(E[V=E'])

```go
x[x := N]       ≡ N
y[x := N]       ≡ y //注意 x ≠ y
(M1 M2)[x := N] ≡ (M1[x := N]) (M2[x := N])
(λx.M)[x := N]  ≡ λx.M //注意 x 是绑定变量无法替换
(λy.M)[x := N]  ≡ λy.(M[x := N]) //注意 x ≠ y, 且表达式N的自由变量中不包含 y 即 y ∉ FV(N)
```

arthmetic function in haskell

- quot 
- rem 取余。
- div
- mod  取模

![image-20210729111850382](Haskell.assets/image-20210729111850382.png)

取模取余运算区别

对于整型数a，b来说，取模运算或者求余运算的方法都是：

1. 求 整数商： c = a/b;

2. 计算模或者余数： r = a - c*b.

求模运算和求余运算在第一步不同: 取余运算在取c的值时，向0 方向舍入(fix()函数)；而取模运算在计算c的值时，向负无穷方向舍入(floor()函数)。

例如：计算-7 Mod 4

那么：a = -7；b = 4；

第一步：求整数商c，如进行求模运算c = -2（向负无穷方向舍入），求余c = -1（向0方向舍入）；

第二步：计算模和余数的公式相同，但因c的值不同，求模时r = 1，求余时r = -3。

归纳：当a和b符号一致时，求模运算和求余运算所得的c的值一致，因此结果一致。

当符号不一致时，结果不一样。求模运算结果的符号和b一致，求余运算结果的符号和a一致。

另外各个环境下%运算符的含义不同，比如**c/c++，java 为取余**，而**python则为取模**。



quot舍入为零，div舍入为负无穷：

rem 舍入为零，mod舍入为负                          无穷

ps. 我没看懂

## list

- 不能存放不同类型
- [1,2] !! 0 获取列表第0个元素

![image-20210730114744364](Haskell.assets/image-20210730114744364.png)

head,tail,init,last



```sh
ghci>task 3 [3,4,5,4] 
[3,4,5]
ghci>task 1 [2,3]
[3]
ghci> drop 3 [8,4,2,1,5,6]
[1,5,6]
ghci> maximum [1,6,2,3]
6
ghci> minimum [8,4,2,1]
1
ghci product [1,2,3]
6 
ghci> 4`elem` [3,4,5,6]
True
```

```sh
ghci> take 10 (cycle [1,2,3])
[1,2,3,1,2,3,1,2,3,1]
ghci> take 10 (repeat 5)
5555555555 10ge
ghci> replicate 3 10
[10,10,10]
```

```sh
ghci> [x*2|x<-[1..10],x*2>=12]
[12,14,16,18,20]
```

## AS-PATTERN

```haskell
firstLetter all@(x:xs) = "The first letter of " ++ all ++ " is " ++ [x]
```



## guard where

![image-20210805104930741](Haskell.assets/image-20210805104930741.png)

## LEt it be

let expressions are very similar to `where`bindings.`where`allows you bind to variables at the end of a functions.and those variable are visible to the entire function.