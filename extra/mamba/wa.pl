#!/usr/bin/perl
#Copyright (c) 2015, Apps4Av Inc. (apps4av@gmail.com) 
#All rights reserved.
#
#Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
#
#    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
#    * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
#
#THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
#
#
# Author: zkhan


use strict;
use warnings;

my $stations="BHM,33Ε33,-86Ε44,HSV,34Ε33,-86Ε46,MGM,32Ε13,-86Ε19,MOB,30Ε41,-88Ε14,ADK,51Ε56,-176Ε25,ADQ,57Ε46,-152Ε35,AKN,58Ε44,-156Ε45,ANC,61Ε14,-149Ε33,ANN,55Ε03,-131Ε37,BET,60Ε35,-161Ε35,BRW,71Ε17,-156Ε31,BTI,70Ε10,-143Ε55,BTT,66Ε54,-151Ε30,CDB,55Ε11,-162Ε22,CZF,61Ε47,-166Ε02,EHM,58Ε39,-162Ε04,FAI,64Ε43,-148Ε11,FYU,66Ε35,-145Ε05,GAL,64Ε44,-156Ε56,GKN,62Ε09,-145Ε27,HOM,59Ε39,-151Ε29,JNU,58Ε26,-134Ε41,LUR,68Ε53,-166Ε07,MCG,62Ε49,-155Ε24,MDO,59Ε30,-146Ε18,OME,64Ε37,-165Ε05,ORT,63Ε04,-142Ε04,OTZ,66Ε39,-162Ε54,SNP,57Ε09,-170Ε37,TKA,62Ε19,-150Ε06,UNK,63Ε53,-160Ε48,YAK,59Ε37,-139Ε30,IKO,52Ε57,-168Ε51,AFM,67Ε06,-157Ε51,5AB,52Ε25,176Ε00,5AC,52Ε00,-135Ε00,5AD,54Ε00,-145Ε00,5AE,55Ε00,-155Ε00,5AF,56Ε00,-137Ε00,5AG,58Ε00,-142Ε00,FSM,35Ε23,-94Ε16,LIT,34Ε40,-92Ε10,PHX,33Ε25,-111Ε53,PRC,34Ε42,-112Ε28,TUS,32Ε07,-110Ε49,BIH,37Ε22,-118Ε21,BLH,33Ε35,-114Ε45,FAT,36Ε53,-119Ε48,FOT,40Ε40,-124Ε14,ONT,34Ε03,-117Ε36,RBL,40Ε05,-122Ε14,SAC,38Ε26,-121Ε33,SAN,32Ε44,-117Ε11,SBA,34Ε30,-119Ε46,SFO,37Ε37,-122Ε22,SIY,41Ε47,-122Ε27,WJF,34Ε44,-118Ε13,ALS,37Ε20,-105Ε48,DEN,39Ε48,-104Ε53,GJT,39Ε03,-108Ε47,PUB,38Ε17,-104Ε25,BDL,41Ε56,-72Ε41,EYW,24Ε35,-81Ε48,JAX,30Ε26,-81Ε33,MIA,25Ε57,-80Ε27,MLB,28Ε06,-80Ε38,PFN,30Ε12,-85Ε40,PIE,27Ε54,-82Ε41,TLH,30Ε33,-84Ε22,ATL,33Ε37,-84Ε26,CSG,32Ε36,-85Ε01,SAV,32Ε09,-81Ε06,ITO,19Ε43,-155Ε03,HNL,21Ε19,-157Ε55,LIH,21Ε59,-159Ε20,OGG,20Ε54,-156Ε26,LNY,20E47,-156Ε57,KOA,19E44,-156Ε03,BOI,43Ε34,-116Ε14,LWS,46Ε22,-116Ε52,PIH,42Ε52,-112Ε39,JOT,41Ε32,-88Ε19,SPI,39Ε50,-89Ε40,EVV,38Ε02,-87Ε31,FWA,40Ε58,-85Ε11,IND,39Ε48,-86Ε22,BRL,40Ε43,-90Ε55,DBQ,42Ε24,-90Ε42,DSM,41Ε26,-93Ε38,MCW,43Ε05,-93Ε19,GCK,37Ε55,-100Ε43,GLD,39Ε23,-101Ε41,ICT,37Ε43,-97Ε27,SLN,38Ε52,-97Ε37,LOU,38Ε06,-85Ε34,LCH,30Ε08,-93Ε06,MSY,30Ε01,-90Ε10,SHV,32Ε46,-93Ε48,BGR,44Ε50,-68Ε52,CAR,46Ε52,-68Ε01,PWM,43Ε38,-70Ε18,EMI,39Ε29,-76Ε58,ACK,41Ε16,-70Ε01,BOS,42Ε21,-70Ε59,ECK,43Ε15,-82Ε43,MKG,43Ε10,-86Ε02,MQT,46Ε31,-87Ε35,SSM,46Ε24,-84Ε18,TVC,44Ε40,-85Ε32,AXN,45Ε57,-95Ε13,DLH,46Ε48,-92Ε12,INL,48Ε33,-93Ε24,MSP,45Ε08,-93Ε22,CGI,37Ε13,-89Ε34,COU,38Ε48,-92Ε13,MKC,39Ε16,-94Ε35,SGF,37Ε21,-93Ε20,STL,38Ε51,-90Ε28,JAN,32Ε30,-90Ε10,BIL,45Ε48,-108Ε37,DLN,45Ε14,-112Ε32,GPI,48Ε12,-114Ε10,GGW,48Ε12,-106Ε37,GTF,47Ε27,-111Ε24,MLS,46Ε22,-105Ε57,HAT,35Ε16,-75Ε33,ILM,34Ε21,-77Ε52,RDU,35Ε52,-78Ε47,DIK,46Ε51,-102Ε46,GFK,47Ε57,-97Ε11,MOT,48Ε15,-101Ε17,BFF,41Ε53,-103Ε28,GRI,40Ε59,-98Ε18,OMA,41Ε10,-95Ε44,ONL,42Ε28,-98Ε41,BML,44Ε38,-71Ε11,ACY,39Ε27,-74Ε34,ABQ,35Ε02,-106Ε48,FMN,36Ε44,-108Ε05,ROW,33Ε20,-104Ε37,TCC,35Ε10,-103Ε35,ZUN,34Ε57,-109Ε09,BAM,40Ε34,-116Ε55,ELY,39Ε17,-114Ε50,LAS,36Ε04,-115Ε09,RNO,39Ε31,-119Ε39,ALB,42Ε44,-73Ε48,BUF,42Ε55,-78Ε38,JFK,40Ε37,-73Ε46,PLB,44Ε48,-73Ε24,SYR,43Ε09,-76Ε12,CLE,41Ε21,-82Ε09,CMH,39Ε59,-82Ε55,CVG,39Ε00,-84Ε42,GAG,36Ε20,-99Ε52,OKC,35Ε24,-97Ε38,TUL,36Ε11,-95Ε47,AST,46Ε09,-123Ε52,IMB,44Ε38,-119Ε42,LKV,42Ε29,-120Ε30,OTH,43Ε24,-124Ε10,PDX,45Ε44,-122Ε35,RDM,44Ε15,-121Ε18,AGC,40Ε16,-80Ε02,AVP,41Ε16,-75Ε41,PSB,40Ε54,-77Ε59,CAE,33Ε51,-81Ε03,CHS,32Ε53,-80Ε02,FLO,34Ε13,-79Ε39,GSP,34Ε53,-82Ε13,ABR,45Ε25,-98Ε22,FSD,43Ε38,-96Ε46,PIR,44Ε23,-100Ε09,RAP,43Ε58,-103Ε00,BNA,36Ε07,-86Ε40,MEM,35Ε03,-89Ε58,TRI,36Ε28,-82Ε24,TYS,35Ε54,-83Ε53,ABI,32Ε28,-99Ε51,AMA,35Ε17,-101Ε38,BRO,25Ε55,-97Ε22,CLL,30Ε36,-96Ε25,CRP,27Ε54,-97Ε26,DAL,32Ε50,-96Ε51,DRT,29Ε22,-100Ε55,ELP,31Ε48,-106Ε16,HOU,29Ε38,-95Ε16,INK,31Ε52,-103Ε14,LBB,33Ε42,-101Ε54,LRD,27Ε28,-99Ε25,MRF,30Ε17,-103Ε37,PSX,28Ε45,-96Ε18,SAT,28Ε38,-98Ε27,SPS,33Ε59,-98Ε35,BCE,37Ε41,-112Ε18,SLC,40Ε51,-111Ε58,ORF,36Ε53,-76Ε12,RIC,37Ε30,-77Ε19,ROA,37Ε20,-80Ε04,GEG,47Ε33,-117Ε37,SEA,47Ε26,-122Ε18,YKM,46Ε34,-120Ε26,GRB,44Ε33,-88Ε11,LSE,43Ε52,-91Ε15,CRW,38Ε20,-81Ε46,EKN,38Ε54,-80Ε05,CZI,43Ε59,-106Ε26,LND,42Ε48,-108Ε43,MBW,41Ε50,-106Ε00,RKS,41Ε35,-109Ε00,2XG,30Ε20,-78Ε30,T01,28Ε30,-93Ε30,T06,28Ε30,-91Ε00,T07,28Ε30,-88Ε00,4J3,28Ε30,-85Ε00,H51,26Ε30,-95Ε00,H52,26Ε00,-89Ε30,H61,26Ε30,-84Ε00,JON,16Ε44,-169Ε32,MAJ,07Ε04,171Ε16,KWA,08Ε43,167Ε44,MDY,28Ε12,-177Ε23,PPG,-14Ε20,-170Ε43,TTK,05Ε21,162Ε58,AWK,19Ε17,166Ε39,GRO,14Ε11,145Ε14,GSN,15Ε07,145Ε44,TNI,15Ε00,145Ε37,GUM,13Ε29,144Ε48,TKK,07Ε28,151Ε51,PNI,06Ε59,158Ε13,ROR,07Ε22,134Ε33,T11,09Ε30,138Ε05";
my $start = 0;
my $line = 0;

my @stationss = split(/,/, $stations);

open(INPUT, "all") or die;

my $valid = "";
while(<INPUT>) {
    chomp($_);
    if($_ =~ /<pre>/) {
        $start = 1;
        next;
    }
    if($_ =~ /<\/pre>/) {
        $start = 0;
        next;
    }
    if($start)  {
        $line++;
        if($line == 5) {
            $_ =~ s/ TEMPS NEG ABV 24000//;
            $valid = $_;
        }
        if($line > 7) {
            my $nm = substr($_,  0, 3);
            my($index) = grep{$stationss[$_] eq $nm } 0..$#stationss;
            $index //= -1;
            if($index < 0) {
                next;
            }
            my $latu = $stationss[$index + 1];
            my @lats = split('Ε', $latu);
            my $lat = $lats[0] + ($lats[1] / 60.0);
            if($lats[0] < 0) {
                $lat = $lats[0] - ($lats[1] / 60.0);
            }
            my $lonu = $stationss[$index + 2];
            my @lons = split('Ε', $lonu);
            my $lon = $lons[0] + ($lons[1] / 60.0);
            if($lons[0] < 0) {
                $lon = $lons[0] - ($lons[1] / 60.0);
            }
            my $k3  = substr($_,  4, 4);
            my $k6  = substr($_,  9, 7);
            my $k9  = substr($_, 17, 7);
            my $k12 = substr($_, 25, 7);
            my $k18 = substr($_, 33, 7);
            my $k24 = substr($_, 41, 7);
            my $k30 = substr($_, 49, 6);
            my $k34 = substr($_, 56, 6);
            my $k39 = substr($_, 63, 6);
            print "$nm,$valid,$lon,$lat,$k3,$k6,$k9,$k12,$k18,$k24,$k30,$k34,$k39\n";
            
        }
    }
}


