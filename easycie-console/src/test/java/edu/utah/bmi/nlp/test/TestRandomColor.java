package edu.utah.bmi.nlp.test;


import edu.utah.bmi.nlp.uima.writer.EhostConfigurator;

/**
 * Created by Jianlin Shi on 9/26/16.
 */
public class TestRandomColor {
    public static void main(String[]args){
        for(int i=0;i<1000;i++){
            EhostConfigurator.getRandomBeautifulColors();
        }
    }
}
