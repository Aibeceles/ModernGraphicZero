# GraphicZero-HigherDegreedP's
> Notebook ID: 2G1TWRHEF

---

So take the maxn=8 set. - it is composed of n 2-7.
each p defined at n computes 2^n and so have a set of p's defining {4,8,16,32,64,128}.

For instance, MaxN=8,n=4 is a set of p's each argumented with rowScalars 1,2,4.  1 and 2 are not defined in MaxN8 though
4 is.  An opportunity exists to substitute 4 with MaxN8N2 (it evaluates to 4) as p/4 * MaxN8N4RowScalar4.

---

..

---

```scala
import org.apache.spark.sql.{DataFrame,Dataset}

val z1= z.get("RootsMaxn8Range").asInstanceOf[DataFrame]
val z1Count=z1.count()
z1.show(z1Count.toInt,false)

val z2= z.get("ReducedMaxN8RangePivot").asInstanceOf[DataFrame]
val z2Count=z2.count()
z2.show(z2Count.toInt,false)

val z3= z.get("ReducedMaxN8RangeResultPivot").asInstanceOf[DataFrame]
val z3Count=z3.count()
z3.show(z3Count.toInt,false)

val z4= z.get("ReducedMaxN8RangeFinalResult").asInstanceOf[DataFrame]
val z4Count=z4.count()
z4.show(z4Count.toInt,false)
```

---

..

---

```scala
val z5= z.get("index7MaxN8RowScalar1").asInstanceOf[DataFrame]
val z5Count=z5.count()
z5.show(z5Count.toInt,false)
```

---

## 0. DataFrame/Dataset case class definitions.

```scala
case class s12QQu(index:String,MaxN:String,rowScalar:String,divisor:String,scalar:String,degree:String)
case class s12Re(dimension:String,degree:String,scalar:String,index:String,maxIndex:String,divisor:String,result:String,rowScalar:String)
case class indexSum(iSum:String,finalISum:String)
case class kvCaseC(rowScalar:String,degree:String,scalar:String)
case class pivotE(N:String,rowScalar:String,zero:String,one:String,two:String)
//case class roots(root1:String,root2:String)
case class roots(N:String,rowScalar:String,root1:String,root2:String,a:String,b:String,c:String)
case class redScal(Scalar:String,MaxN:String,N:String,Degree:String)
case class redScalR(Scalar:String,N:String,Degree:String,result:String,MaxN:String)
```

---

## 1. QuerryS12QuadQuerry object.

```scala

object QuerryS12QuadQuerry {
  
import zadscripts.DFScripts
import scala.collection.mutable.ArrayBuffer
import scala.math.BigDecimal
import org.apache.spark.sql.types.DecimalType

val s3 = new DFScripts 

def s12QuadQuerry(rLow:String,rHigh:String,nMax:String): ArrayBuffer[s12QQu] = {

val terms = new ArrayBuffer[s12QQu]()
//val s3 = new DFScripts  
val s12QQRS = s3.s12QuadQ(rLow,rHigh,nMax)

while (s12QQRS.next()) {
terms += s12QQu(s12QQRS.getString(1),s12QQRS.getString(2),s12QQRS.getString(3),s12QQRS.getString(4),s12QQRS.getString(5),s12QQRS.getString(6))
}
terms
}

def main(args:Array[String]) {

val terms = new ArrayBuffer[ArrayBuffer[s12QQu]]()
val nMax = args(0)                                 
//for(a <- 1 to 1; n <- 7 to 7;b <- 0 to (n-2)) {
for(a <- 1 to 1; n <- 2 to BigInt(args(0)).intValue()-1) {   //;b <- 0 to 0
     terms += s12QuadQuerry(n.toString,n.toString,nMax)
}

val mainTermss=terms.flatMap(_.toList)

val df = spark.createDataFrame(mainTermss)
val dfCount = df.count()
df.show(dfCount.toInt,false)
z.put(args(1),df)
}
}
```

---

## 2. QuerryS12QuadQuerry calls.

```scala
QuerryS12QuadQuerry.main(Array("8","s12MaxN8")) 
```

---

## 3. ResultUDF Object

```scala
object ResultUDF extends Serializable {

import zadscripts.DFScripts
import org.apache.spark.sql.{DataFrame,Dataset}
import scala.collection.mutable.ArrayBuffer
import java.math.BigDecimal
import org.apache.spark.sql.types.DecimalType
import spark.implicits._

def extendResult(qQuerry:s12QQu): s12Re = {      // need to include the rowCounter multiplication
  val scalar = new BigDecimal(qQuerry.scalar)
  val rowScalar = new BigDecimal(qQuerry.rowScalar)
  val degree = qQuerry.degree.toInt
  val divisor = new BigDecimal(qQuerry.divisor)
  val index = new BigDecimal(qQuerry.MaxN)
  val indexPower = index.pow(degree)
  val scalarDiv = scalar.divide(divisor)
  val result = indexPower.multiply(scalarDiv)
  val result1 = result.multiply(new BigDecimal(qQuerry.rowScalar))
  val s=  s12Re("2",qQuerry.degree,qQuerry.scalar,qQuerry.index,qQuerry.MaxN,qQuerry.divisor,result1.toPlainString,qQuerry.rowScalar)
  s
}

def main(args:Array[String]) {
    
//import org.apache.spark.sql.DataFrame
val df= z.get(args(0)).asInstanceOf[DataFrame]
spark.udf.register("eResult", extendResult _)

val ds: Dataset[s12QQu] = df.as[s12QQu]
//val dss=ds.map{f => extendResult(s3AQu(f.dimension,f.Degree,f.scalar,f.index,f.maxIndex,f.divisor)) }

val dss=ds.map{f => extendResult(s12QQu(f.index,f.MaxN,f.rowScalar,f.divisor,f.scalar,f.degree)) }
val dssCount=dss.count()
dss.show(dssCount.toInt,false)
z.put(args(1),dss)

}
}
```

---

## 4. ResultUDF object Calls.

```scala
ResultUDF.main(Array("s12MaxN8","rUdfMaxN8"))
```

---

## 9. GroupedScalarRowsSum Aggrigator (for scalars)

```scala
//case class s12QQu(index:String,MaxN:String,rowScalar:String,divisor:String,scalar:String,degree:String)
//case class s12Re(dimension:String,degree:String,scalar:String,index:String,maxIndex:String,divisor:String,result:String,rowScalar:String)
//case class indexSum(iSum:String,finalISum:String)


import org.apache.spark.sql.{Encoder, Encoders}
import org.apache.spark.sql.expressions.Aggregator
import spark.implicits._
import org.apache.spark.sql.Dataset
import scala.math.BigDecimal
import org.apache.spark.sql.types.DecimalType

object GroupedScalarRowsSum extends Aggregator[s12Re, indexSum, String] {

  def zero: indexSum = indexSum("0", "0")
  def reduce(buffer: indexSum, s3TR: s12Re): indexSum = {
    val red1= BigDecimal(buffer.iSum)+BigDecimal(s3TR.scalar)
    val red2=buffer.finalISum  
    indexSum(red1.bigDecimal.toPlainString,red2)
  }
  def merge(b1: indexSum, b2: indexSum): indexSum = {
    val mer1=BigDecimal(b1.iSum)+BigDecimal(b2.iSum)
    val mer2=b1.finalISum
    indexSum(mer1.bigDecimal.toPlainString,mer2)
  }
  def finish(reduction: indexSum): String = reduction.iSum
  def bufferEncoder: Encoder[indexSum] = Encoders.product
  def outputEncoder: Encoder[String] = Encoders.STRING
//}


def main(args:Array[String]) {

val ds= z.get(args(0)).asInstanceOf[Dataset[s12Re]]
                                                                                                 // val extendResultDSCoun=ds.count()
val scalarResult = GroupedScalarRowsSum.toColumn.name("scalar_Result")
val kDS= ds.groupByKey(x=>(x.index,x.rowScalar.toInt,x.degree.toInt))
   .agg(scalarResult)
//   .orderBy(asc("key"))
val kdsCount=kDS.count()
kDS.show(kdsCount.toInt,false)
kDS.schema                                                                                      //  val iResult = ds.select(indexResult)
z.put(args(1),kDS)

}

}                                                                                                //  iResult.show()
```

---

## 10. GroupedScalarRowsSum object calls.

```scala
GroupedScalarRowsSum.main(Array("rUdfMaxN8","KvDS"))
```

---

## 11. Unpack Grouped DataFrame keys.

```scala
object MapKVUDF extends Serializable {

import zadscripts.DFScripts
import org.apache.spark.sql.{DataFrame,Dataset,Row}
import scala.collection.mutable.ArrayBuffer
import java.math.BigDecimal
import org.apache.spark.sql.types.DecimalType
import spark.implicits._

def main(args:Array[String]) {

val df= z.get(args(0)).asInstanceOf[DataFrame]
//df.schema                                                           
//df.show()

val kDS = df.select($"*", $"key.*")
kDS.show
z.put(args(1),kDS)


}
}
```

---

## 12. MapKVDF object call.

```scala
MapKVUDF.main(Array("KvDS","KvDSUnpavked"))
```

---

## 13. RowScalar,DegreePivot object.

```scala
object RowScalarDegreePivot extends Serializable {

import org.apache.spark.sql.DataFrame
import org.apache.spark.sql.types.DecimalType

def main(args:Array[String]) {

val df= z.get(args(0)).asInstanceOf[DataFrame]
//df.show
val groupDf = df.withColumn("N",col("_1").cast(DecimalType(25,0)))
                .withColumn("rowScalar",col("_2").cast(DecimalType(25,0)))
                .withColumn("degree",col("_3").cast(DecimalType(25,0)))
                .withColumn("scalar",col("scalar_Result").cast(DecimalType(25,0)))
                .orderBy(asc("N"),asc("rowScalar"))
                .groupBy("N","rowScalar")
                .pivot("degree")
                .agg(sum("scalar"))
val groupDfCount = groupDf.count()
groupDf.show(groupDfCount.toInt,false)
groupDf.schema
z.put(args(1),groupDf)

}
}
```

---

## 14. RowScalarDegreePivot object calls.

```scala
RowScalarDegreePivot.main(Array("KvDSUnpavked","pivotDF"))
```

---

## 15. PivotEvaluateUDF object implements Quadratic Equation.

```scala
object PivotEvaluateUDF extends Serializable {

//case class s12QQu(index:String,MaxN:String,rowScalar:String,divisor:String,scalar:String,degree:String)
//case class s12Re(dimension:String,degree:String,scalar:String,index:String,maxIndex:String,divisor:String,result:String,rowScalar:String)
//case class indexSum(iSum:String,finalISum:String)
//case class s3ARe(dimension:String,Degree:String,scalar:String,index:String,maxIndex:String,divisor:String,result:String)
//case class pivotE(rowScalar:DecimalType,zero:DecimalType,one:DecimalType,two:DecimalType)
//case class roots(rowScalar:String,root1:String,root2:String,a:String,b:String,c:String)

import zadscripts.DFScripts
import org.apache.spark.sql.{DataFrame,Dataset}
import scala.collection.mutable.ArrayBuffer
import java.math.BigDecimal
import org.apache.spark.sql.types.DecimalType
import spark.implicits._
import org.apache.spark.sql.{Encoder, Encoders}

def quadEqu(rowP:pivotE): roots = {      //           need to include the rowCounter multiplication  and remember all are BigDecimal.

  val four = new BigDecimal(4)
  val zero = new BigDecimal(0)
  val two = new BigDecimal(2)
  val rowScalar = new BigDecimal(rowP.rowScalar)
  val a = new BigDecimal(rowP.two)
  val aMM = rowScalar.multiply(a)
  val aM = aMM.divide(two)
  
  val twoA = aM.multiply(two)
  val b = new BigDecimal(rowP.one)
  val bMM = rowScalar.multiply(b)
  val bM = bMM.divide(two)
  
  val negB = zero.subtract(bM)
  val negBdTwoA = negB.divide(twoA)
  val bSqr = bM.pow(2)
  val c = new BigDecimal(rowP.zero)
  val cMM = rowScalar.multiply(c)
  val cM = cMM.divide(two)
  
  val ac= aM.multiply(cM)
  val fac = ac.multiply(four)
  val disc = bSqr.subtract(fac)
  val discD = disc.doubleValue()    
  if (discD>0) {
    val discSqr = Math.sqrt(discD)
    val discSqrBD = new BigDecimal(discSqr)
    val discSqrdTwoA = discSqrBD.divide(twoA)
    val rootOne = negBdTwoA.add(discSqrdTwoA)
    val rootTwo = negBdTwoA.subtract(discSqrdTwoA)
    val s = roots(rowP.N,rowP.rowScalar,rootOne.toPlainString(),rootTwo.toPlainString(),rowP.two,rowP.one,rowP.zero)
    s
    } else 
    {
    val s = roots(rowP.N,rowP.rowScalar,"no ","root ",rowP.two,rowP.one,rowP.zero)
    s
    }
 }

def main(args:Array[String]) {
//val df= z.get(args(0)).asInstanceOf[DataFrame]
spark.udf.register("qEque", quadEqu _)
val dfff= z.get(args(0)).asInstanceOf[DataFrame]
//dfff.show(false)
//val ds: Dataset[pivotE] = df.as[pivotE]
val dss =dfff.map(row => pivotE(row.getDecimal(0).toPlainString(), row.getDecimal(1).toPlainString(), row.getDecimal(2).toPlainString(), row.getDecimal(3).toPlainString(),row.getDecimal(4).toPlainString()))
//dss.show(false)
val dsss=dss.map{f => quadEqu(pivotE(f.N,f.rowScalar,f.zero,f.one,f.two)) }
//dss.show(false)
//val dss=ds.map{f => extendResult(s12QQu(f.index,f.MaxN,f.rowScalar,f.divisor,f.scalar,f.degree)) }
val dsssCount=dsss.count()
dsss.show(dsssCount.toInt,false)
z.put(args(1),dsss)
}
}
```

---

## 16. PivotEvaluateUDF object calls.

```scala
PivotEvaluateUDF.main(Array("pivotDF","RootsMaxn8Range"))
```

---

## 17. GroupedDegreeSum object (aggrigate by degree across all rowScalars)

```scala
import org.apache.spark.sql.{Encoder, Encoders}
import org.apache.spark.sql.expressions.Aggregator
import spark.implicits._
import org.apache.spark.sql.Dataset
import java.math.BigDecimal
import org.apache.spark.sql.types.DecimalType

object GroupedDegreeSum extends Aggregator[s12Re, indexSum, String] {

  def zero: indexSum = indexSum("0", "0")

  def reduce(buffer: indexSum, s3TR: s12Re): indexSum = {
    val iSum = new BigDecimal(buffer.iSum)
    val divisor = new BigDecimal(s3TR.divisor)
    val scalar = new BigDecimal(s3TR.scalar)
    val rowScalar = new BigDecimal(s3TR.rowScalar)
    val sRScalar = scalar.multiply(rowScalar)
    val dsRScalar = sRScalar.divide(divisor)                               
    val red1 = iSum.add(dsRScalar)
    val red2=buffer.finalISum  
    indexSum(red1.toPlainString,red2)
  }

  def merge(b1: indexSum, b2: indexSum): indexSum = {
    val b1isum = new BigDecimal(b1.iSum)
    val b2isum = new BigDecimal(b2.iSum)
    val mer1= b1isum.add(b2isum)
    val mer2=b1.finalISum
    indexSum(mer1.toPlainString,mer2)
  }

  def finish(reduction: indexSum): String = reduction.iSum
  def bufferEncoder: Encoder[indexSum] = Encoders.product
  def outputEncoder: Encoder[String] = Encoders.STRING

def main(args:Array[String]) {

val ds= z.get(args(0)).asInstanceOf[Dataset[s12Re]]
val scalarResult = GroupedDegreeSum.toColumn.name("scalar_Result")
val kDS= ds.groupByKey(x=>(x.maxIndex,x.index.toInt,x.degree.toInt))
   .agg(scalarResult)
//   .orderBy(asc("key"))
val kdsCount=kDS.count()
kDS.show(kdsCount.toInt,false)
kDS.schema
z.put(args(1),kDS)

}
} 
```

---

## 18. GroupedDegreeSum object calls.

```scala
GroupedDegreeSum.main(Array("rUdfMaxN8","KvDegreeDS"))
```

---

## 19. Unpack GroupedDegreeSum result DataFrame.

```scala
MapKVUDF.main(Array("KvDegreeDS","KvDegreeDSUnpavked"))
```

---

## 20. DropKeyColumn object (clean up key,value dataframe)

```scala
object DropKeyColumn extends Serializable {

import org.apache.spark.sql.DataFrame
import org.apache.spark.sql.types.StringType

def main(args:Array[String]) {

val df= z.get(args(0)).asInstanceOf[DataFrame]
val dropedDf = df.withColumn("MaxN",col("_1").cast(StringType))
                 .withColumn("N",col("_2").cast(StringType))
                 .withColumn("Degree",col("_3").cast(StringType))
                 .withColumn("Scalar",col("scalar_Result").cast(StringType))
                 .orderBy(asc("N"),asc("Degree"))
                 .drop("key")
                 .drop("_1")
                 .drop("_2")
                 .drop("_3")
                 .drop("scalar_result")
dropedDf.show                
z.put(args(1),dropedDf)

}
}
```

---

## 21. DropKeyColumn object call.

```scala
DropKeyColumn.main(Array("KvDegreeDSUnpavked","ReducedMaxN8Range" ))
```

---

## 21.5 Pivot 21 (for narrative).

```scala
object AggrigatedScalarDegreePivot extends Serializable {

import org.apache.spark.sql.DataFrame
import org.apache.spark.sql.types.DecimalType

def main(args:Array[String]) {

val dh= z.get(args(0)).asInstanceOf[DataFrame]
// dh.show
val groupDff = dh.withColumn("N",col("N").cast(DecimalType(25,0)))
                .withColumn("Degree",col("Degree").cast(DecimalType(25,0)))
                .withColumn("Scalar",col("Scalar").cast(DecimalType(25,0)))
                .orderBy(asc("N"),asc("Degree"))
                .groupBy("N")
                .pivot("Degree")
                .agg(sum("Scalar"))
val groupDfCount = groupDff.count()
groupDff.show(groupDfCount.toInt,false)
groupDff.schema
z.put(args(1),groupDff)

}
}
```

---

## 21.50 AggrigatedScalarDegreePivot call.

```scala
AggrigatedScalarDegreePivot.main(Array("ReducedMaxN8Range","ReducedMaxN8RangePivot"))
```

---

## 22. ReducedResultUDF object (usual term calculate).

```scala
object ReducedResultUDF extends Serializable {

//case class redScal(Scalar:String,MaxN:String,N:String,Degree:String)
//case class redScalR(Scalar:String,N:String,Degree:String,result:String,MaxN:String)

import zadscripts.DFScripts
import org.apache.spark.sql.{DataFrame,Dataset}
import scala.collection.mutable.ArrayBuffer
import java.math.BigDecimal
import org.apache.spark.sql.types.DecimalType
import spark.implicits._

def extendResult(qQuerry:redScal): redScalR = {      // need to include the rowCounter multiplication
  val scalar = new BigDecimal(qQuerry.Scalar)
  val degre = qQuerry.Degree.toInt
  val degree = degre.toInt
  val index = new BigDecimal(qQuerry.MaxN)
  val indexPower = index.pow(degree)
  val result = indexPower.multiply(scalar)
  val s=  redScalR(scalar.toPlainString,qQuerry.N,qQuerry.Degree,result.toPlainString,qQuerry.MaxN)
  s
}

def main(args:Array[String]) {
    
import org.apache.spark.sql.DataFrame
val dp= z.get(args(0)).asInstanceOf[DataFrame]
spark.udf.register("eResult", extendResult _)

val dpp =dp.map(row => redScal(row.getString(3), row.getString(0), row.getString(1),row.getString(2)))


val dppp=dpp.map{f => extendResult(redScal(f.Scalar,f.MaxN,f.N,f.Degree)) }

dppp.show(false)


z.put(args(1),dppp)

}
}
```

---

## 23. ReducedResultUDF object call.

```scala
ReducedResultUDF.main(Array("ReducedMaxN8Range","ReducedMaxN8RangeResult"))
```

---

## 23.5 ReducedResultUDFPivot object

```scala
object ReducedResultUDFPivot extends Serializable {

import org.apache.spark.sql.DataFrame
import org.apache.spark.sql.types.DecimalType

def main(args:Array[String]) {

val dhh= z.get(args(0)).asInstanceOf[DataFrame]
// dh.show
val groupDfff = dhh.withColumn("MaxN",col("MaxN").cast(DecimalType(25,0)))
                   .withColumn("N",col("N").cast(DecimalType(25,0)))
                   .withColumn("Degree",col("Degree").cast(DecimalType(25,0)))
                   .withColumn("Result",col("result").cast(DecimalType(25,0)))
//                  .orderBy(asc("nBD"),asc("rowCounterBD"))
                   .groupBy("MaxN","N")
                   .pivot("Degree")
                   .agg(sum("Result"))
val groupDfCount = groupDfff.count()
groupDfff.show(groupDfCount.toInt,false)
groupDfff.schema
z.put(args(1),groupDfff)

}
}
```

---

## ReducedResultUDFPivot object calls.

```scala
ReducedResultUDFPivot.main(Array("ReducedMaxN8RangeResult","ReducedMaxN8RangeResultPivot"))
```

---

## 24. ReducedRowsSum object.

```scala
object ReducedRowsSum extends Aggregator[redScalR, indexSum, String] {

import org.apache.spark.sql.{Encoder, Encoders, SparkSession}
import org.apache.spark.sql.expressions.Aggregator
import spark.implicits._
import org.apache.spark.sql.{DataFrame,Dataset,Row}
import scala.math.BigDecimal

  def zero: indexSum = indexSum("0", "0")
  def reduce(buffer: indexSum, s3TR: redScalR): indexSum = {
    val red1= BigDecimal(buffer.iSum)+BigDecimal(s3TR.result)
    val red2=buffer.finalISum  
    indexSum(red1.bigDecimal.toPlainString,red2)
  }
  
  def merge(b1: indexSum, b2: indexSum): indexSum = {
    val mer1=BigDecimal(b1.iSum)+BigDecimal(b2.iSum)
    val mer2=b1.finalISum
    indexSum(mer1.bigDecimal.toPlainString,mer2)
  }
 
  def finish(reduction: indexSum): String = reduction.iSum
  def bufferEncoder: Encoder[indexSum] = Encoders.product
  def outputEncoder: Encoder[String] = Encoders.STRING

def main(args:Array[String]) {

val de= z.get(args(0)).asInstanceOf[DataFrame]
val dee: Dataset[redScalR] = de.as[redScalR]


val indexResult = ReducedRowsSum.toColumn.name("index_Result")
val iR24=dee.groupByKey(x=>(x.MaxN,x.N))
            .agg(indexResult)
val iR24Count=iR24.count()
iR24.show(iR24Count.toInt,false)

//val iResult = dee.select(indexResult)
//iResult.show(false)
z.put(args(1),iR24)

}
}
```

---

## 25. ReducedRowsSum object call.

```scala
ReducedRowsSum.main(Array("ReducedMaxN8RangeResult","ReducedMaxN8RangeFinalResult"))
```
