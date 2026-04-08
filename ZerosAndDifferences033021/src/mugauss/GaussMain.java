/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package mugauss;

import LoopLists.GaussBean1;
import LoopLists.LoopList;
import java.util.*;
import java.io.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.StringTokenizer;
import java.util.concurrent.Semaphore;
import mucorrolationthreaded.GaussBean;

public class GaussMain{


    


   // swap()
   // swap row i with row k
   // pre: A[i][q]==A[k][q]==0 for 1<=q<j
   static void swap(double[][] A, int i, int k, int j){
      int m = A[0].length - 1;
      double temp;
      for(int q=j; q<=m; q++){
         temp = A[i][q];
         A[i][q] = A[k][q];
         A[k][q] = temp;
      }
   }

   // divide()
   // divide row i by A[i][j]
   // pre: A[i][j]!=0, A[i][q]==0 for 1<=q<j
   // post: A[i][j]==1;
   static void divide(double[][] A, int i, int j){
      int m = A[0].length - 1;
      for(int q=j+1; q<=m; q++) A[i][q] /= A[i][j];
      A[i][j] = 1;
   }

   // eliminate()
   // subtract an appropriate multiple of row i from every other row
   // pre: A[i][j]==1, A[i][q]==0 for 1<=q<j
   // post: A[p][j]==0 for p!=i
   static void eliminate(double[][] A, int i, int j){
      int n = A.length - 1;
      int m = A[0].length - 1;
      for(int p=1; p<=n; p++){
         if( p!=i && A[p][j]!=0 ){
            for(int q=j+1; q<=m; q++){
               A[p][q] -= A[p][j]*A[i][q];
            }
            A[p][j] = 0;
         }
      }
   }

   // printMatrix()
   // print the present state of Matrix A to file out
   void newGaussBean(List gList, Semaphore s11, double[][] A,BigDecimal trZero,BigDecimal trTr, BigDecimal root1, BigDecimal root2, BigDecimal root3) {
  
      GaussBean gBean= new GaussBean(A[1][1],A[1][4],A[2][2],A[2][4],A[3][3],A[3][4],trZero,trTr,root1,root2,root3);
       synchronized(this) {   //latest thread issue
       gList.add(gBean);
//   System.out.println("new gauss bean    new gauss bean");
       s11.release();
       }
  }
   
   
   public  void printMatrix( double[][] A){
     int n = A.length - 1;
     int m = A[0].length - 1;
     
     for(int i=0; i<=n; i++){                                         //  indices start at zero or one.
         for(int j=0; j<=m; j++) System.out.print(A[i][j] + "  ");
         System.out.println();
      }
      
      System.out.println();
      System.out.println();
   }
   // main()
   // origonally read input file, ust take matrix directly
   // forget output file... to be persisted
   public void gauss(List gList, Semaphore s1,double[][] A, int n,int m,BigDecimal trZero,BigDecimal trTr, BigDecimal root1, BigDecimal root2, BigDecimal root3){
      int i, j, k;
      String line;
 //     StringTokenizer st;


   
      
  //    BufferedReader in = new BufferedReader(new FileReader(args[0]));
  //    PrintWriter out = new PrintWriter(new FileWriter(args[1]));

      // read first line of input file
 //     line = in.readLine();
 //     st = new StringTokenizer(line);
 //     n = Integer.parseInt(st.nextToken());
 //     m = Integer.parseInt(st.nextToken());

      // declare A to be of size (n+1)x(m+1) and do not use index 0
 //     double[][] A = new double[n+1][m+1];

      // read next n lines of input file and initialize array A
 //     for(i=1; i<=n; i++){
 //        line = in.readLine();
 //        st = new StringTokenizer(line);
 //        for(j=1; j<=m; j++){
 //           A[i][j] = Double.parseDouble(st.nextToken());
 //        }
 //     }

      // close input file
 //     in.close();

      // print array A to output file
 //     printMatrix(out, A);

      // perform Gauss-Jordan Elimination algorithm
      i = 1;
      j = 1;
//System.out.println("i: " + i + " j: " + j + " n: " + n + " m: " + m);
//System.out.println("A: " + A.length + " A: " + A.toString() + " n: " + n + " m: " + m);
      while( i<=n && j<=m ){
//System.out.println("while   i: " + i + " j: " + j + " n: " + n + " m: " + m);
         //look for a non-zero entry in col j at or below row i
         k = i;
   //       System.out.println("   k: " + k + " n: " + n + " j: " + j + " m: " + m);
         while( k<=n && A[k][j]==0 ) { k++;
  //           System.out.println("while   k: " + k + " n: " + n + " j: " + j + " m: " + m);
         }
         // if such an entry is found at row k
         if( k<=n ){

            //  if k is not i, then swap row i with row k
            if( k!=i ) {
               swap(A, i, k, j);
   //            printMatrix(out, A);
            }

            // if A[i][j] is not 1, then divide row i by A[i][j]
            if( A[i][j]!=1 ){
               divide(A, i, j);
     //          printMatrix(out, A);
            }

            // eliminate all other non-zero entries from col j by subtracting from each
            // row (other than i) an appropriate multiple of row i
            eliminate(A, i, j);
//           printMatrix(A);
            i++;
         }
         j++;
      }
 //  System.out.println("new gauss                                     bean " +gList.toString());
      newGaussBean(gList, s1, A,trZero,trTr,root1, root2, root3);

      // print rank to output file
  //    out.println("rank = " + (i-1));

      // close output file
  //    out.close();
       
   }
   
   public void gauss(int n,int m,Semaphore s1,double[][] A){
      int i, j, k;
      String line;
      System.out.println("Old GaussMain.gauss");
 //     printMatrix(A);
      // perform Gauss-Jordan Elimination algorithm
      i = 1;
      j = 1;
//System.out.println("i: " + i + " j: " + j + " n: " + n + " m: " + m);
//System.out.println("A: " + A.length + " A: " + A.toString() + " n: " + n + " m: " + m);
      while( i<=n && j<=m ){
//System.out.println("while   i: " + i + " j: " + j + " n: " + n + " m: " + m);
         //look for a non-zero entry in col j at or below row i
         k = i;
   //       System.out.println("   k: " + k + " n: " + n + " j: " + j + " m: " + m);
         while( k<=n && A[k][j]==0 ) { k++;
  //           System.out.println("while   k: " + k + " n: " + n + " j: " + j + " m: " + m);
         }
         // if such an entry is found at row k
         if( k<=n ){

            //  if k is not i, then swap row i with row k
            if( k!=i ) {
               swap(A, i, k, j);
   //            printMatrix(out, A);
            }

            // if A[i][j] is not 1, then divide row i by A[i][j]
            if( A[i][j]!=1 ){
               divide(A, i, j);
     //          printMatrix(out, A);
            }

            // eliminate all other non-zero entries from col j by subtracting from each
            // row (other than i) an appropriate multiple of row i
            eliminate(A, i, j);
   //        printMatrix(A);
            i++;
         }
         j++;
      }
   //System.out.println("new gauss                                     bean " +gList.toString());
 //     newGaussBean(gList, s1, A,trZero,trTr,root1, root2, root3);

      // print rank to output file
  //    out.println("rank = " + (i-1));

      // close output file
  //    out.close();
       
   }   
 
 
   
   private void transcribeResult(double[][]A,GaussBean1 gBean1,int n, int m) {
   List buffer = new ArrayList();
   LoopList lList;
//   System.out.println("GaussMain.transcribeResult               00                     m: " + buffer.toString()+ "  " + m);
   for (int x=0;  x<m; x++){ 
      buffer.add(A[x][m]);
//     System.out.println("GaussMain.transcribeResult               00   11                  transcribeResult: " + x);
   }    
   gBean1.setVmResult(buffer.toString());
//  System.out.println("GaussMain.transcribeResult                 11                   transcribeResult: " + buffer.toString()+" gBean1.getVmResult:   "+gBean1.getVmResult());
   lList=gBean1.getLoopList();     // slick, follow the looplist from looplistener.  And yee will understand passby reference.
    lList.setVmResult(buffer.toString());
   
   }
   
   
   
   public void gauss(List gList, Semaphore s1,int n,int m,double[][] A,GaussBean1 gBean1){
      int i, j, k;
      String line;
 //     StringTokenizer st;

//  System.out.println(" Enter gauss pArray, n, m  "+  gBean1.getpArray()+" "+n+" "+m );
//  printMatrix(A);
//  System.out.println(" Enter gauss A[0][].size  "+  A[0].length ); 
  m = A[0].length-1;   

      i = 1;
      j = 1;
//System.out.println("A: ");this.printMatrix(A);
//System.out.println("i: " + i + " j: " + j + " n: " + n + " m: " + m);
//System.out.println("A: " + A.length  + " n: " + n + " m: " + m);
      while( i<=n && j<=m ){
//System.out.println("while   i: " + i + " j: " + j + " n: " + n + " m: " + m);
         //look for a non-zero entry in col j at or below row i
         k = i;
   //  System.out.println("   k: " + k + " n: " + n + " j: " + j + " m: " + m);
         while( k<=n && A[k][j]==0 ) { k++;
   //       System.out.println("while   k: " + k + " n: " + n + " j: " + j + " m: " + m);
         }
         // if such an entry is found at row k
         if( k<=n ){

            //  if k is not i, then swap row i with row k
            if( k!=i ) {
               swap(A, i, k, j);
//              printMatrix(A);
            }

            // if A[i][j] is not 1, then divide row i by A[i][j]
            if( A[i][j]!=1 ){
               divide(A, i, j);
//               printMatrix( A);
            }

            // eliminate all other non-zero entries from col j by subtracting from each
            // row (other than i) an appropriate multiple of row i
            eliminate(A, i, j);
//           printMatrix(A);
            i++;
         }
         j++;
      }
//  System.out.println("                                                                                gauss fallthrough ");
//  printMatrix(A);
  transcribeResult(A,gBean1,n,m);

//       synchronized(this) {   //latest thread issue
//       gList.add(gBean1);
// //  System.out.println("                                                                new gauss bean    new gauss bean");
//       s1.release();
//       }
// System.out.println("                                                               gauss fall through");
//     newGaussBean(gList, s1, A,trZero,trTr,root1, root2, root3);
      // print rank to output file
  //    out.println("rank = " + (i-1));
      // close output file
  //    out.close();
       
   }   
   
   
}






