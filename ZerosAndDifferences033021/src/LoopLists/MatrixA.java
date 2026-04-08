/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package LoopLists;

import java.math.BigDecimal;
import java.util.List;

/**
 *
 * @author Aibes
 */
public class MatrixA {
double [][] A;
double [][] xPowers;
double [][] gMatrix;          //  the matrix with index 0 =0 for gauss algorythm
int i;


public MatrixA(MatrixA origonal) {
//  System.out.println();
//  System.out.println("MatrixA Copy Constructor");
  try {
//  System.out.println("A dimensions: "+ ((double[][])origonal.A).length+ " "+((double[][])origonal.A)[0].length );
  A=new double[((double[][])origonal.A).length][((double[][])origonal.A)[i].length];
  for(int i=0; i<((double[][])origonal.A).length; i++) {
  for(int j=0; j<((double[][])origonal.A)[i].length; j++)
  {
    ((double[][])this.A)[i][j]=((double[][])origonal.A)[i][j];
 //   System.out.println( "New A "     +  (((double[][])this.A)[i][j])   );
  }  
//    System.out.println();
  }  
  xPowers=new double[((double[][])origonal.xPowers).length][((double[][])origonal.xPowers)[i].length];
  for(int i=0; i<((double[][])origonal.xPowers).length; i++) {
  for(int j=0; j<((double[][])origonal.xPowers)[i].length; j++)
  {
    ((double[][])this.xPowers)[i][j]=((double[][])origonal.xPowers)[i][j];
 //   System.out.println( "new xPower"     +  (((double[][])this.xPowers)[i][j])   );
  }  
//    System.out.println();
  }
  gMatrix=new double[((double[][])origonal.gMatrix).length][((double[][])origonal.gMatrix)[i].length];
  for(int i=0; i<((double[][])origonal.gMatrix).length; i++) {
  for(int j=0; j<((double[][])origonal.gMatrix)[i].length; j++)
  {
    ((double[][])this.gMatrix)[i][j]=((double[][])origonal.gMatrix)[i][j];
 //   System.out.println( "new gMatrix"     +  (((double[][])this.gMatrix)[i][j])   );
  }  
//    System.out.println();
  }
} catch (ArrayIndexOutOfBoundsException | NullPointerException ex) {
        
}
  
  this.i=origonal.i;  
}



public MatrixA(int i, int j)
{
 //  System.out.println("MatrixA(int i, int j) constructor  ");
    
   A=new double[i+1][j+1];    
   gMatrix= new double[i+1][j+2];
   this.i=i;
}    

public MatrixA(int i)       //  this constructor is only used for initial generation of powersMatrix
{    
 //  System.out.println("MatrixA(int i) constructor  ");
   this.i=i;
   A= new double [i+1][i+2];         // this matrix is never used
   gMatrix=new double[i+1][i+3];
   xPowers= new double [i+1][i+1];
   this.powersMatrix();
} 

public MatrixA(int i, int j, MatrixA xPowers)
{   //this.aMatrix= new MatrixA(mDimension,mDimension+1,aMatrix);
//   System.out.println("MatrixA(int i, int j, MatrixA xPowers) constructor, i: "+i+ "  " + j);    
   A=new double[i][i+1];
   gMatrix= new double[i+1][i+2];
   this.i=i;
   this.xPowers=xPowers.getXPowers();
//   System.out.println("matrixA constructor, xPowers.xPowers.Length: "+xPowers.xPowers.length);
   this.transcribePowers();
}


public double [][] getXPowers() {
    
  return(this.xPowers);
}

public double [][] getA() {
    
  return(this.A);
}

public double [][] getgMatrix() {
    
  return(this.gMatrix);
}

private void powersMatrix() {
    double xPower;
    double resultX;
    for (int x=0;  x<this.i+1; x++){ 
      this.xPowers[x][0]=1;
      xPower=x+1;
      resultX=xPower;

      for (int y=1; y<this.i+1; y++){ 
//      System.out.println("powersMatrix loop y" + y);
      this.xPowers[x][y]=resultX;
      resultX=resultX*xPower;
//      System.out.println("powersMatrix" +this.xPowers[x][y] + "  ");
      }
    }
//    printMatrix();
  }

private void transcribePowers() {

int denumerator=this.i;
double xPower;

 //    System.out.println("MatrixA.transcribePowers  this.i: " +this.i);

for (int x=0;  x<this.i; x++){ 
    
   denumerator=this.i-1;
    
   for (int y=0; y<this.i; y++){

//       System.out.println("MatrixA.transcribePowers  pre" +this.xPowers[x][denumerator]+"  " + x + "  "+ y);

//        printMatrix();
  //    System.out.println("Pre this.A[x][y]=this.xPowers[x][denumerator];: ");
        this.A[x][y]=this.xPowers[x][denumerator];
 //       System.out.println("Pre this.A[x][y]=this.xPowers[x][denumerator];: ");
        this.gMatrix[x+1][y+1]=this.xPowers[x][denumerator];
       denumerator=denumerator-1;
   }
 }
//System.out.println("MatrixA.transcribePowers printMatrix" );    
//this.printMatrix();

//System.out.println("End MatrixA.transcribePowers printMatrix" ); 

//System.out.println("MatrixA.transcribePowers this.i "+this.i );
//System.out.println("MatrixA.transcribePowers x loop "+this.i  + " x: "+x);
//System.out.println("MatrixA.transcribePowers x loop, denumerator "+denumerator );
//System.out.println("MatrixA.transcribePowers y loop "+ y); 
//System.out.println("MatrixA.transcribePowers  a.length: " +this.A.length + "  "+ y);
//System.out.println("MatrixA.transcribePowers post" + this.A[x][y]);


}



public void setMatrix(List matrixList) {
//    System.out.println("setMatrixList  " + matrixList.toString()+ "  ");
    for (int x=0;  x<this.i; x++){ 
//         System.out.println("setMatrixList  " + ((BigDecimal)matrixList.get(x)).doubleValue());
//    A[x][(this.i)]=((BigDecimal)matrixList.get(x+1)).doubleValue();                                      // do not want zero value in vandermonde
//    gMatrix[x+1][(this.i)+1]=((BigDecimal)matrixList.get(x+1)).doubleValue();                              // also, due to wierd sercomstances using second index   
A[x][(this.i)]=((BigDecimal)matrixList.get(100+x+1)).doubleValue(); 
gMatrix[x+1][(this.i)+1]=((BigDecimal)matrixList.get(100+x+1)).doubleValue();    
    }
//System.out.println("i: " + (i+1) );    
//System.out.println("setMatrixList  printMatrix");

//this.printMatrix();

//System.out.println("setMatrixList  end printMatrix");

} 

public void printMatrix(){
//     int n = this.A.length - 1;
//     int m = this.A[0].length - 1;
      for(int d=0; d<this.i; d++){
         for(int j=0; j<(this.i)+1; j++){
     //       System.out.println("printMatrix d,j "+d+ " "+j+" "+this.i);
     //       System.out.println(this.A.toString() + "  ");
       //                  System.out.print(this.A[d][j] + "  ");
  //           System.out.print(this.A[d][j] + "  ");
         
         }
//         System.out.println();
      }
 //     System.out.println();
  //    System.out.println();

      for(int d=0; d<i; d++){
         for(int j=0; j<i; j++) System.out.print(this.xPowers[d][j] + "  ");
//         System.out.println();
      }
 //     System.out.println();
 //     System.out.println();
      
         for(int d=0; d<this.i+1; d++){
         for(int j=0; j<(this.i)+2; j++){
     //       System.out.println("printMatrix d,j "+d+ " "+j+" "+this.i);
     //       System.out.println(this.A.toString() + "  ");
       //                  System.out.print(this.A[d][j] + "  ");
 //            System.out.print(this.gMatrix[d][j] + "  ");
         
         }
 //        System.out.println();
      }
 //     System.out.println();
 //     System.out.println();
   }





}
