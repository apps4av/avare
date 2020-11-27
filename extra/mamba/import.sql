.separator ","

CREATE TABLE airsig(
raw_text Text,
valid_time_from Text,
valid_time_to Text, 
point Text,
min_ft_msl Text,
max_ft_msl Text,
movement_dir_degrees Text,
movement_speed_kt Text,
hazard Text,
severity Text,
airsigmet_type Text);

.import airsig.csv airsig

CREATE TABLE apirep(
raw_text Text,
observation_time Text,
longitude float,
latitude float,
report_type Text);

.import apirep.csv apirep


CREATE TABLE tafs(
raw_text Text,
issue_time Text,
station_id Text);

.import tafs.csv tafs

CREATE TABLE metars(
raw_text Text,
issue_time Text,
station_id Text,
flight_category Text,
longitude float,
latitude float);

.import metars.csv metars

CREATE TABLE wa(
stationid Text,
valid Text,
longitude float,
latitude float,
w3k Text,
w6k Text,
w9k Text,
w12k Text,
w18k Text,
w24k Text,
w30k Text,
w34k Text,
w39k Text);

.import wa.csv wa
