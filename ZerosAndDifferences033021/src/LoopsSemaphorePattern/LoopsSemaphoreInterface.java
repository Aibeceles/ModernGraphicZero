/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
3
67.77*/
package LoopsSemaphorePattern;

/**
 *
 * @author HP_Administrator
 */

public interface LoopsSemaphoreInterface {
 
    void sf2Aquire(); 
    void sb2Release();    
    void sf1Release();
    void sb1Aquire();    
    void rentrentReset();
}
