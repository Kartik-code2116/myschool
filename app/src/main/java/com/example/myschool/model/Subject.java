package com.example.myschool.model;

public class Subject {
    public String name;
    public int maxMarks;

    // Optional customized sub-field max marks (Formative / आकारिक)
    public int maxNirikhshan;
    public int maxTondiKam;
    public int maxPratyakshik;
    public int maxUpkram;
    public int maxPrakalp;
    public int maxChachani;
    public int maxSwadhyay;
    public int maxItar;

    // Optional customized sub-field max marks (Summative / संकलित)
    public int maxTondi;
    public int maxPratyakshikB;
    public int maxLekhi;

    public Subject() {}

    public Subject(String name, int maxMarks) {
        this.name = name;
        this.maxMarks = maxMarks;
        
        // Auto-scale defaults
        int akarikMax  = maxMarks / 2;
        int sanklitMax = maxMarks - akarikMax;

        this.maxNirikhshan   = akarikMax * 10 / 50;
        this.maxTondiKam     = akarikMax * 10 / 50;
        this.maxPratyakshik  = akarikMax * 10 / 50;
        this.maxUpkram       = akarikMax * 5  / 50;
        this.maxPrakalp      = akarikMax * 5  / 50;
        this.maxChachani     = akarikMax * 5  / 50;
        this.maxSwadhyay     = akarikMax * 5  / 50;
        this.maxItar         = akarikMax - this.maxNirikhshan - this.maxTondiKam - this.maxPratyakshik
                - this.maxUpkram - this.maxPrakalp - this.maxChachani - this.maxSwadhyay;

        this.maxTondi        = sanklitMax * 10 / 50;
        this.maxPratyakshikB = sanklitMax * 10 / 50;
        this.maxLekhi        = sanklitMax - this.maxTondi - this.maxPratyakshikB;
    }
}
