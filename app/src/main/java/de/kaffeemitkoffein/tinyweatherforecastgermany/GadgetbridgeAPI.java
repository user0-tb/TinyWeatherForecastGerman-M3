/*
 * This file is part of TinyWeatherForecastGermany.
 *
 * Copyright (c) 2020 Pawel Dube
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.kaffeemitkoffein.tinyweatherforecastgermany;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import nodomain.freeyourgadget.gadgetbridge.model.WeatherSpec;

public class GadgetbridgeAPI {

    public final static String WEATHER_EXTRA="WeatherSpec";
    public final static String WEATHER_ACTION="de.kaffeemitkoffein.broadcast.WEATHERDATA";
    public final static double KelvinConstant = 273.15;

    private WeatherSpec weatherSpec;
    private Context context;

    public GadgetbridgeAPI(Context context){
        this.context = context;
    }

    private int toKelvin(double temperature){
        return (int) (temperature - KelvinConstant);
    }

    private void setWeatherData(){
        WeatherForecastContentProvider weatherForecastContentProvider = new WeatherForecastContentProvider();
        WeatherCard weatherCard = weatherForecastContentProvider.readWeatherForecast(context);
        // build the WeatherSpec instance with current weather
        WeatherCodeContract weatherCodeContract = new WeatherCodeContract(weatherCard,0,0);
        weatherCodeContract.setLineageOsCompatible(true);
        int currentWeatherCondition = weatherCodeContract.getWeatherCondition();
        weatherSpec = new WeatherSpec();
        weatherSpec.currentConditionCode = currentWeatherCondition;
        weatherSpec.currentCondition     = weatherCodeContract.getWeatherConditionText(context,currentWeatherCondition);
        weatherSpec.currentTemp          = toKelvin(weatherCard.getCurrentTemp());
        weatherSpec.location             = weatherCard.getName();
        weatherSpec.timestamp            = (int) (weatherCard.polling_time / 1000);
        weatherSpec.todayMaxTemp         = toKelvin(weatherCard.todaysHigh());
        weatherSpec.todayMinTemp         = toKelvin(weatherCard.todaysLow());
        weatherSpec.windSpeed            = (float) weatherCard.getCurrentWindSpeed();
        weatherSpec.windDirection        = (int) weatherCard.getCurrentWindDirection();
        // build the forecast instance
        weatherCodeContract = new WeatherCodeContract(weatherCard,WeatherCodeContract.WEATHER_24H);
        weatherCodeContract.setLineageOsCompatible(true);
        int forecastCondition = weatherCodeContract.getWeatherCondition();
        WeatherSpec.Forecast forecast = new WeatherSpec.Forecast(toKelvin(weatherCard.get24hLow()),
                                                                 toKelvin(weatherCard.get24hLow()),
                                                                 forecastCondition,
                                                         0);
        weatherSpec.forecasts.add(forecast);
        /*
        Log.v("GADGETBRIDGE-API","Timestamp          : "+weatherSpec.timestamp);
        Log.v("GADGETBRIDGE-API","Condition          : "+weatherSpec.currentCondition);
        Log.v("GADGETBRIDGE-API","Temperature current: "+weatherSpec.currentTemp);
        Log.v("GADGETBRIDGE-API","Temperature min    : "+weatherSpec.todayMinTemp);
        Log.v("GADGETBRIDGE-API","Temperature max    : "+weatherSpec.todayMaxTemp);
        Log.v("GADGETBRIDGE-API","FC-Temperature max : "+weatherSpec.forecasts.get(0).minTemp);
        Log.v("GADGETBRIDGE-API","FC-Temperature max : "+weatherSpec.forecasts.get(0).maxTemp);
        Log.v("GADGETBRIDGE-API","FC-Condition       : "+weatherSpec.forecasts.get(0).conditionCode);
        Log.v("GADGETBRIDGE-API","Windspeed          : "+weatherSpec.windSpeed);
        Log.v("GADGETBRIDGE-API","Windspeed direct.  : "+weatherSpec.windDirection);
        */
    }

    private final void sendWeatherBroadcast(){
        WeatherSettings weatherSettings = new WeatherSettings(context);
        setWeatherData();
        Intent intent = new Intent();
        intent.putExtra(WEATHER_EXTRA,weatherSpec);
        // going by the docs, this requires at least api level 14
        // read the package name from the settings. Users may change the package name to
        // be able to use forks.
        intent.setPackage(weatherSettings.gadgetbridge_packagename);
        intent.setFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
        intent.setAction(WEATHER_ACTION);
        context.sendBroadcast(intent);
    }

    public final void sendWeatherBroadcastIfEnabled(){
        WeatherSettings weatherSettings = new WeatherSettings(context);
        if (weatherSettings.serve_gadgetbridge){
            sendWeatherBroadcast();
        }
    }

}

