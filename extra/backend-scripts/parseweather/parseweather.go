package main

import (
    "encoding/xml"
	"encoding/csv"
	"bufio"
	"os"
	"io"
	"log"
    "fmt"
    "io/ioutil"
	"strings"
    "time"
	"strconv"
    "database/sql"
    _ "github.com/mattn/go-sqlite3"	
)

type ADDSMetarData struct {
    RequestIndex int    `xml:"request_index"`
    DataList     []MetarData `xml:"data"`
}

type ADDSTAFData struct {
    RequestIndex int    `xml:"request_index"`
    DataList     []TAFData `xml:"data"`
}

type ADDSPIREPData struct {
    RequestIndex int    `xml:"request_index"`
    DataList     []PIREPData `xml:"data"`
}

type ADDSSIGData struct {
    RequestIndex int    `xml:"request_index"`
    DataList     []SIGData `xml:"data"`
}

type MetarData struct {
    Metars []METAR `xml:"METAR"`
}

type TAFData struct {
	TAFs []TAF `xml:"TAF"`
}

type PIREPData struct {
	PIREPs []PIREP `xml:"AircraftReport"`
}

type SIGData struct {
	SIGs []SIG `xml:"AIRSIGMET"`
}

type PIREP struct {
    ObservationTime time.Time `xml:"observation_time"`
    Lat             float64   `xml:"latitude"`
    Lng             float64   `xml:"longitude"`
    RawText         string    `xml:"raw_text"`
	ReportType		string	  `xml:"report_type"`
}

type METAR struct {
    StationID       string    `xml:"station_id"`
    Lat             float64   `xml:"latitude"`
    Lng             float64   `xml:"longitude"`
    RawText         string    `xml:"raw_text"`
    ObservationTime time.Time `xml:"observation_time"`
}

type TAF struct {
	StationID		string	  `xml:"station_id"`
	IssueTime       string    `xml:"issue_time"`
	RawText         string    `xml:"raw_text"`
}


type SIG struct {
	RawText			string		`xml:"raw_text"`
	ValidFrom		string		`xml:"valid_time_from"`
	ValidTo			string		`xml:"valid_time_to"`
	Area		 []SigArea		`xml:"area"`
	Altitude		SigAltitude	`xml:"altitude"`
	MovementDirDeg	string		`xml:"movement_dir_degrees"`
	MovementSpeedKt	string		`xml:"movement_speed_kt"`
	Hazard			SigHazard	`xml:"hazard"`
	Severity		string		`xml:"severity"`
	SigType			string		`xml:"airsigmet_type"`
	PointStr		string
}

type SigAltitude struct {
	MinFtMSL	string	`xml:"min_ft_msl,attr"`
	MaxFtMSL	string	`xml:"max_ft_msl,attr"`
}

type SigHazard struct {
		Type	string	`xml:"type,attr"`
		Severity string `xml:"severity"`
}
 
type SigArea struct {
	Points	[]SIGPoint	`xml:"point"`
}

type SIGPoint struct {
	Lng		string		`xml:"longitude"`
	Lat		string		`xml:"latitude"`
}

type WA struct {
	StationID	string		
	Valid	string			
	Lng		float64			
	Lat		float64			
	w3k		string			
	w6k		string			
	w9k		string			
	w12k	string			
	w18k	string			
	w24k	string			
	w30k	string			
	w34k	string			
	w39k	string			
}

var WeatherTables string = `
DROP TABLE metars;
DROP TABLE airsig;
DROP TABLE apirep;
DROP TABLE tafs;
DROP TABLE wa;
CREATE TABLE IF NOT EXISTS metars( raw_text Text, issue_time Text, station_id Text, flight_category Text, longitude float, latitude float);
CREATE TABLE IF NOT EXISTS airsig( raw_text Text, valid_time_from Text, valid_time_to Text, point Text, min_ft_msl Text, max_ft_msl Text, movement_dir_degrees Text, movement_speed_kt Text, hazard Text, severity Text, airsigmet_type Text);
CREATE TABLE IF NOT EXISTS apirep( raw_text Text, observation_time Text, longitude float, latitude float, report_type Text);
CREATE TABLE IF NOT EXISTS tafs( raw_text Text, issue_time Text, station_id Text);
CREATE TABLE IF NOT EXISTS wa( stationid Text, valid Text, longitude float, latitude float, w3k Text, w6k Text, w9k Text, w12k Text, w18k Text, w24k Text, w30k Text, w34k Text, w39k Text);
`

func check(e error) {
    if e != nil {
        panic(e)
    }
}

func SpeedUpDatabase(db *sql.DB) {
	_,err := db.Exec("PRAGMA synchronous = OFF")
	if err != nil { panic(err) }
}

func CreateTables(db *sql.DB, sql_table string) {
	// create table if not exists

	_, err := db.Exec(sql_table)
	if err != nil { panic(err) }
}


func StoreMetarItem(db *sql.DB, Entry METAR) {
	str := `
	INSERT OR REPLACE INTO metars ( 
		raw_text,
		issue_time,
		station_id,
		flight_category,
		longitude,
		latitude
		) values(?, ?, ?, ?, ?, ?);`
	
	stmt, err := db.Prepare(str)
	if err != nil { 
		panic(err) 
	}
	defer stmt.Close()
	_, err2 := stmt.Exec(Entry.RawText, Entry.ObservationTime, Entry.StationID, "VFR", Entry.Lng, Entry.Lat)
	if err2 != nil { 
		panic(err2) 
	}
}

func StoreTAFItem(db *sql.DB, Entry TAF) {
	str := `
	INSERT OR REPLACE INTO tafs ( 
		raw_text,
		issue_time,
		station_id
		) values(?, ?, ?);`
	
	stmt, err := db.Prepare(str)
	if err != nil { 
		panic(err) 
	}
	defer stmt.Close()
	_, err2 := stmt.Exec(Entry.RawText, Entry.IssueTime, Entry.StationID)
	if err2 != nil { 
		panic(err2) 
	}
}

func StorePIREPItem(db *sql.DB, Entry PIREP) {
	str := `
	INSERT OR REPLACE INTO apirep ( 
		raw_text,
		observation_time,
		longitude,
		latitude,
		report_type
		) values(?, ?, ?, ?, ?);`
	
	stmt, err := db.Prepare(str)
	if err != nil { 
		panic(err) 
	}
	defer stmt.Close()
	_, err2 := stmt.Exec(Entry.RawText, Entry.ObservationTime, Entry.Lng, Entry.Lat, Entry.ReportType)
	if err2 != nil { 
		panic(err2) 
	}
}

func StoreSIGItem(db *sql.DB, Entry SIG) {
	str := `
	INSERT OR REPLACE INTO airsig ( 
		raw_text,
		valid_time_from,
		valid_time_to,
		point,
		min_ft_msl,
		max_ft_msl,
		movement_dir_degrees,
		movement_speed_kt,
		hazard,
		severity,
		airsigmet_type
		) values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);`
	
	stmt, err := db.Prepare(str)
	if err != nil { 
		panic(err) 
	}
	defer stmt.Close()
	var PointStr string
	PointStr = ""
	for pnt := range Entry.Area[0].Points {
		PointEntry := Entry.Area[0].Points[pnt]
		PointStr = PointStr + PointEntry.Lng+":"+PointEntry.Lat
		if pnt < len(Entry.Area[0].Points)-1 {
			PointStr = PointStr + ";"
		}
	}
	Entry.PointStr = PointStr
	_, err2 := stmt.Exec(Entry.RawText, Entry.ValidFrom, Entry.ValidTo, Entry.PointStr, Entry.Altitude.MinFtMSL, Entry.Altitude.MaxFtMSL, Entry.MovementDirDeg, Entry.MovementSpeedKt, Entry.Hazard.Type, Entry.Hazard.Severity, Entry.SigType)
	if err2 != nil { 
		panic(err2) 
	}
}

//CREATE TABLE IF NOT EXISTS wa( stationid Text, valid Text, longitude float, latitude float, w3k Text, w6k Text, w9k Text, w12k Text, w18k Text, w24k Text, w30k Text, w34k Text, w39k Text);

func StoreWAItem(db *sql.DB, Entry WA) {
	str := `
	INSERT OR REPLACE INTO wa ( 
		stationid,
		valid,
		longitude,
		latitude,
		w3k,
		w6k,
		w9k,
		w12k,
		w18k,
		w24k,
		w30k,
		w34k,
		w39k
		) values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);`
	
	stmt, err := db.Prepare(str)
	if err != nil { 
		panic(err) 
	}
	defer stmt.Close()
	_, err2 := stmt.Exec(Entry.StationID, Entry.Valid, Entry.Lng, Entry.Lat, Entry.w3k, Entry.w6k, Entry.w9k, Entry.w12k, Entry.w18k, Entry.w24k, Entry.w30k, Entry.w34k, Entry.w39k)
	if err2 != nil { 
		panic(err2) 
	}
}

func ParseMetarData(db *sql.DB) {
    var data ADDSMetarData
    dat, err := ioutil.ReadFile("./xml/metars.xml")
    check(err)
    err = xml.Unmarshal(dat, &data)
    if err != nil {
        fmt.Printf("err: %s\n", err.Error())
    }

	for val := range data.DataList[0].Metars {
		MetarEntry := data.DataList[0].Metars[val]
		fmt.Printf("Storing METAR for %s\n",MetarEntry.StationID)
		StoreMetarItem(db, MetarEntry)
	}
}

func ParseTAFSData(db *sql.DB) {
    var data ADDSTAFData
    dat, err := ioutil.ReadFile("./xml/tafs.xml")
    check(err)
    err = xml.Unmarshal(dat, &data)
    if err != nil {
        fmt.Printf("err: %s\n", err.Error())
    }

	for val := range data.DataList[0].TAFs {
		TAFEntry := data.DataList[0].TAFs[val]
		fmt.Printf("Adding TAF for %s\n",TAFEntry.StationID)
		StoreTAFItem(db, TAFEntry)
	}
}

func ParsePIREPData(db *sql.DB) {
    var data ADDSPIREPData
    dat, err := ioutil.ReadFile("./xml/aircraftreports.xml")
    check(err)
    err = xml.Unmarshal(dat, &data)
    if err != nil {
        fmt.Printf("err: %s\n", err.Error())
    }

	for val := range data.DataList[0].PIREPs {
		PIREPEntry := data.DataList[0].PIREPs[val]
		fmt.Printf("Adding PIREP %s \n",PIREPEntry.RawText)
		StorePIREPItem(db, PIREPEntry)
	}
}

func ParseSIGData(db *sql.DB) {
    var data ADDSSIGData
    dat, err := ioutil.ReadFile("./xml/airsigmets.xml")
    check(err)
    err = xml.Unmarshal(dat, &data)
    if err != nil {
        fmt.Printf("err: %s\n", err.Error())
    }
	for val := range data.DataList[0].SIGs {
		SIGEntry := data.DataList[0].SIGs[val]
		fmt.Printf("Handling SIG %s\n",SIGEntry.RawText)
		StoreSIGItem(db, SIGEntry)
	}
}

func GetAirportLngLat(Designator string) (bool, string, string) {
	file, err := os.Open("./airportlist.txt")
	check(err)
	defer file.Close()
	reader := csv.NewReader(file)
	for {
		record, err := reader.Read() 
		if err == io.EOF {
			return false, "", ""
		} else if err != nil {
			fmt.Println("Error:",err)
			return false, "", ""
		} else {
			if Designator == record[0] {
//				fmt.Printf("Record: Designator %s Lon %s Lat %s\n",record[0],record[1],record[2])
				return true, record[1], record[2]
			}
		}
	}
	return false, "", ""
}

func ParseWinds(db *sql.DB) {
	var entry WA
//	var entries []WA
	var validFlag bool
	var done bool
	var validStr string
    file, err := os.Open("./xml/winds.txt")
	check(err)
	defer file.Close()
	validFlag = false
	done = false
	scanner := bufio.NewScanner(file)
	for scanner.Scan() {
		linein := scanner.Text()
		if(done == false) && len(linein) > 1 {
			words := strings.Fields(linein)
			// We have not encounted the valid flag yet, so check for it
			if validFlag == false {
				if words[0] == "VALID" {
					validFlag = true
					validStr = linein
				}
			} else {
				if words[0] != "" {
//					fmt.Printf("Line: ")
//					fmt.Println(linein)
					// Valid flag is true, so lets parse
					if words[0] == "</pre>" {
						done = true
					} else {
						if words[0] != "FT" {
						// We have a valid point here. Lets parse it
							Airport := words[0]
							found, lon, lat := GetAirportLngLat(Airport)
							if found == true {
								numEntries := len(words)
								entryOffset := numEntries-1
								entry.StationID = Airport
								entry.Valid = validStr
								entry.Lng,_ = strconv.ParseFloat(lon,64)
								entry.Lat,_ = strconv.ParseFloat(lat,64)
								if entryOffset > 0 {
									entry.w39k = words[entryOffset]
									entryOffset--
								}
								if entryOffset > 0 {
									entry.w34k = words[entryOffset]
									entryOffset--
								}
								if entryOffset > 0 {
									entry.w30k = words[entryOffset]
									entryOffset--
								}
								if entryOffset > 0 {
									entry.w24k = words[entryOffset]
									entryOffset--
								}
								if entryOffset > 0 {
									entry.w18k = words[entryOffset]
									entryOffset--
								}
								if entryOffset > 0 {
									entry.w12k = words[entryOffset]
									entryOffset--
								}
								if entryOffset > 0 {
									entry.w9k = words[entryOffset]
									entryOffset--
								}
								if entryOffset > 0 {
									entry.w6k = words[entryOffset]
									entryOffset--
								}
								if entryOffset > 0 {
									entry.w3k = words[entryOffset]
									entryOffset--
								}
								StoreWAItem(db, entry)
							}
						}
					}
				}
			}
		}
	}
	if err := scanner.Err(); err != nil {
		log.Fatal(err)
	}
	if done == false {
		log.Fatal("Trouble parsing winds aloft")
	}
	fmt.Printf("Validstr: %s\n",validStr)
	GetAirportLngLat("ENW")
}

func main() {
    db, err := sql.Open("sqlite3", "./output/weather.db")
//    db, err := sql.Open("sqlite3", "./jim.db")
    check(err)	
	SpeedUpDatabase(db)
	CreateTables(db, WeatherTables)
	ParseMetarData(db)
	ParseTAFSData(db)
	ParsePIREPData(db)
	ParseSIGData(db)
	ParseWinds(db)
}