/**
 * diqube: Distributed Query Base.
 *
 * Copyright (C) 2015 Bastian Gloeckle
 *
 * This file is part of diqube data examples.
 *
 * diqube data examples are free software: you can redistribute it
 * and/or modify it under the terms of the GNU Affero General Public
 * License as published by the Free Software Foundation, either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.diqube.hadoop;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import org.diqube.data.column.ColumnType;
import org.diqube.util.Pair;
import org.diqube.util.Triple;

/**
 * Helper class to maintain changes to the PUMS input files - provide better column names and meaningful values.
 *
 * @author Bastian Gloeckle
 */
public class PumsAdjust {
  private static final Map<String, Triple<String, ColumnType, Function<String, Object>>> colInfo = new HashMap<>();

  /**
   * Return a Function which can change the value of a specific column to a meaningful value.
   * 
   * @param colName
   *          is the original colname, not the one returned by {@link #getNiceColName(String)}.
   */
  public static Function<String, Object> getAdjustFunc(String colName) {
    try {
      Function<String, Object> res = colInfo.get(colName).getRight();
      if (res == null)
        throw new RuntimeException("No adjust func for col " + colName);
      return res;
    } catch (NullPointerException e) {
      System.err.println("No colinfo available for col " + colName);
      throw e;
    }
  }

  /**
   * Return a readable column name for the given original column name.
   */
  public static String getNiceColName(String origColName) {
    try {
      return colInfo.get(origColName).getLeft();
    } catch (NullPointerException e) {
      System.err.println("No colinfo available for col " + origColName);
      throw e;
    }
  }

  /**
   * Return a readable column name for the given original column name.
   */
  public static ColumnType getColType(String origColName) {
    try {
      return colInfo.get(origColName).getMiddle();
    } catch (NullPointerException e) {
      System.err.println("No colinfo available for col " + origColName);
      throw e;
    }
  }

  public static class ReplaceFn implements Function<String, Object> {

    private Map<String, Object> replace = new HashMap<>();

    public ReplaceFn(Replace... replace) {
      for (Replace r : replace)
        this.replace.put(r.getLeft(), r.getRight());
    }

    @Override
    public Object apply(String t) {
      if (replace.containsKey(t))
        return replace.get(t);
      return t;
    }
  }

  public static class Replace extends Pair<String, Object> {
    public Replace(String a, Object b) {
      super(a, b);
    }
  }

  private static Replace R(String a, Object b) {
    return new Replace(a, b);
  }

  private static Function<String, Object> longFn() {
    return new Function<String, Object>() {
      @Override
      public Object apply(String t) {
        try {
          return Long.valueOf(t);
        } catch (NumberFormatException e) {
          return -1L;
        }
      }
    };
  }

  private static Function<String, Object> longSpecial(Replace... special) {
    Map<String, Object> specialMap = new HashMap<>();
    for (Replace s : special)
      specialMap.put(s.getLeft(), s.getRight());

    return new Function<String, Object>() {
      @Override
      public Object apply(String t) {
        if (specialMap.containsKey(t))
          return specialMap.get(t);
        return longFn().apply(t);
      }

    };
  }

  private static Function<String, Object> bool() {
    return longFn();
  }

  static {
    colInfo.put("ACR",
        new Triple<>("lot_size", ColumnType.STRING,
            new ReplaceFn( //
                R("b", "n/a"), R("1", "House on less than one acre"), R("2", "House on one to less than ten acres"),
                R("3", "House on ten or more acres"))));
    colInfo.put("ADJHSG", new Triple<>("adjust_housing", ColumnType.LONG, longFn()));
    colInfo.put("ADJINC", new Triple<>("adjust_income", ColumnType.LONG, longFn()));
    colInfo.put("AGEP", new Triple<>("age", ColumnType.LONG, longFn()));
    colInfo.put("AGS",
        new Triple<>("sales_agriculture_dollar_up_to", ColumnType.LONG,
            longSpecial(//
                R("b", -1L), R("1", 0L), R("2", 999L), R("3", 2499L), R("4", 4999L), R("5", 9999L),
                R("6", Long.MAX_VALUE))));
    colInfo.put("ANC", new Triple<>("ancestry_recode", ColumnType.STRING, new ReplaceFn( //
        R("1", "Single"), R("2", "Multiple"), R("3", "Unclassified"), R("4", "Not reported"))));
    // TODO repeated!
    colInfo.put("ANC1P",
        new Triple<>("ancestry_1", ColumnType.STRING,
            new ReplaceFn(R("001", "Alsatian"), R("003", "Austrian"),
                R("005", "Basque"), R("008", "Belgian"), R("009", "Flemish"), R("011", "British"), R("012",
                    "British Isles"),
                R("020", "Danish"), R("021", "Dutch"), R("022", "English"), R("024", "Finnish"), R("026", "French"),
                R("032", "German"), R("040", "Prussian"), R("046", "Greek"), R("049", "Icelander"), R("050", "Irish"),
                R("051", "Italian"), R("068", "Sicilian077"), R("077", "Luxemburger"), R("078", "Maltese"),
                R("082", "Norwegian"), R("084", "Portuguese"), R("087", "Scotch Irish"), R("088", "Scottish"),
                R("089", "Swedish"), R("091", "Swiss"), R("097", "Welsh"), R("098", "Scandinavian"), R("099", "Celtic"),
                R("100", "Albanian"), R("102", "Belorussian"), R("103", "Bulgarian"), R("109", "Croatian"),
                R("111", "Czech"), R("112", "Bohemian"), R("114", "Czechoslovakian"), R("115", "Estonian"),
                R("122", "German Russian"), R("124", "Rom"), R("125", "Hungarian"), R("128", "Latvian"),
                R("129", "Lithuanian"), R("130", "Macedonian"), R("142", "Polish"), R("144", "Romanian"),
                R("148", "Russian"), R("152", "Serbian"), R("153", "Slovak"), R("154", "Slovene"),
                R("170", "Georgia CIS"), R("171", "Ukrainian"), R("176", "Yugoslavian"), R("177", "Herzegovinian"),
                R("178", "Slavic"), R("179", "Slavonian"), R("183", "Northern European"), R("187", "Western European"),
                R("190", "Eastern European"), R("195", "European"), R("200", "Spaniard"), R("210", "Mexican"),
                R("211", "Mexican American"), R("212", "Mexicano"), R("213", "Chicano"),
                R("215", "Mexican American Indian"), R("218", "Mexican State"), R("221", "Costa Rican"),
                R("222", "Guatemalan"), R("223", "Honduran"), R("224", "Nicaraguan"), R("225", "Panamanian"),
                R("226", "Salvadoran"), R("227", "Central American"), R("231", "Argentinean"), R("232", "Bolivian"),
                R("233", "Chilean"), R("234", "Colombian"), R("235", "Ecuadorian"), R("236", "Paraguayan"),
                R("237", "Peruvian"), R("238", "Uruguayan"), R("239", "Venezuelan"), R("249", "South American"),
                R("250", "Latin American"), R("251", "Latin"), R("252", "Latino"), R("261", "Puerto Rican"),
                R("271", "Cuban"), R("275", "Dominican"), R("290", "Hispanic"), R("291", "Spanish"),
                R("295", "Spanish American"), R("300", "Bahamian"), R("301", "Barbadian"), R("302", "Belizean"),
                R("308", "Jamaican"), R("310", "Dutch West Indian"), R("314", "Trinidadian Tobagonian"),
                R("322", "British West Indian"), R("325", "Antigua and Barbuda"), R("329", "Grenadian"),
                R("330", "Vincent-Grenadine Islander"), R("331", "St Lucia Islander"), R("335", "West Indian"),
                R("336", "Haitian"), R("359", "Other West Indian"), R("360", "Brazilian"), R("370", "Guyanese"),
                R("400", "Algerian"), R("402", "Egyptian"), R("406", "Moroccan"), R("416", "Iranian"),
                R("417", "Iraqi"),
                R("419", "Israeli"), R("421", "Jordanian"), R("425", "Lebanese"), R("429", "Syrian"), R("431",
                    "Armenian"),
                R("434", "Turkish"), R("435", "Yemeni"), R("442", "Kurdish"), R("465", "Palestinian"),
                R("483", "Assyrian"), R("484", "Chaldean"), R("490", "Mideast"), R("495", "Arab"), R("496", "Arabic"),
                R("499", "Other Arab"), R("508", "Cameroon"), R("510", "Cape Verdean"), R("522", "Ethiopian"),
                R("523", "Eritrean"), R("529", "Ghanian"), R("534", "Kenyan"), R("541", "Liberian"),
                R("553", "Nigerian"), R("564", "Senegalese"), R("566", "Sierra Leonean"), R("568", "Somalian"),
                R("570", "South African"), R("576", "Sudanese"), R("587", "Other Subsaharan African"),
                R("598", "Western African"), R("599", "African"), R("600", "Afghan"), R("603", "Bangladeshi"),
                R("609", "Nepali"), R("615", "Asian Indian"), R("618", "Bengali"), R("620", "East Indian"),
                R("650", "Punjab"), R("680", "Pakistani"), R("690", "Sri Lankan"), R("700", "Burmese"),
                R("703", "Cambodian"), R("706", "Chinese"), R("707", "Cantonese"), R("712", "Mongolian"),
                R("720", "Filipino"), R("730", "Indonesian"), R("740", "Japanese"), R("748", "Okinawan"),
                R("750", "Korean"), R("765", "Laotian"), R("768", "Hmong"), R("770", "Malaysian"), R("776", "Thai"),
                R("782", "Taiwanese"), R("785", "Vietnamese"), R("793", "Eurasian"), R("794", "Amerasian"),
                R("795", "Asian"), R("799", "Other Asian"), R("800", "Austrailian"), R("803", "New Zealander"),
                R("808", "Polynesian"), R("811", "Hawaiian"), R("814", "Samoan"), R("815", "Tongan"),
                R("820", "Micronesian"), R("821", "Guamanian"), R("822", "Chamorro Islander"), R("841", "Fijian"),
                R("850", "Pacific Islander"), R("899", "Other Pacific"), R("900", "Afro American"), R("901", "Afro"),
                R("902", "African American"), R("903", "Black"), R("904", "Negro"), R("907", "Creole"),
                R("913", "Central American Indian"), R("914", "South American Indian"), R("917", "Native American"),
                R("918", "Indian"), R("919", "Cherokee"), R("920", "American Indian"), R("921", "Aleut"),
                R("922", "Eskimo"), R("924", "White"), R("925", "Anglo"), R("927", "Appalachian"),
                R("929", "Pennsylvania German"), R("931", "Canadian"), R("935", "French Canadian"), R("936", "Acadian"),
                R("937", "Cajun"), R("939", "American or United States"), R("983", "Texas"), R("994", "North American"),
                R("995", "Mixture"), R("996", "Uncodable entries"), R("997", "Other groups"),
                R("998", "Other responses"), R("999", "Not reported"))));
    colInfo.put("ANC2P", new Triple<>("ancestry_2", ColumnType.STRING, colInfo.get("ANC1P").getRight()));
    colInfo.put("BDS", new Triple<>("bedrooms", ColumnType.LONG, longSpecial(R("b", -1L))));
    colInfo.put("BLD",
        new Triple<>("building", ColumnType.STRING,
            new ReplaceFn(//
                R("bb", "n/a"), R("01", "Mobile home or trailer"), R("02", "One-family house detached"),
                R("03", "One-family house attached"), R("04", "2 Apartments"), R("05", "3-4 Apartments"),
                R("06", "5-9 Apartments"), R("07", "10-19 Apartments"), R("08", "20-49 Apartments"),
                R("09", "50 or more apartments"), R("10", "Boat, RV, van, etc."))));
    colInfo.put("BUS", new Triple<>("business_or_medical_on_property", ColumnType.LONG, longSpecial(//
        R("b", -1L), R("1", 1L), R("2", 0L))));
    colInfo.put("CIT",
        new Triple<>("citizenship_status", ColumnType.STRING,
            new ReplaceFn( //
                R("1", "Born in the U.S."),
                R("2", "Born in Puerto Rico, Guam, the U.S. Virgin Islands, or the Northern Marianas"),
                R("3", "Born abroad of American parent(s)"), R("4", "U.S. citizen by naturalization"),
                R("5", "Not a citizen of the U.S."))));
    colInfo.put("CONP", new Triple<>("condo_fee", ColumnType.LONG, longSpecial(R("bbbb", -1L))));
    colInfo.put("COW", new Triple<>("class_of_worker", ColumnType.STRING, new ReplaceFn(//
        R("b", "n/a"), R("1", "Employee of a private for-profit company"),
        R("2", "Employee of a private not-for-profit"), R("3", "Local government employee"),
        R("4", "Local government employee"), R("5", "Federal government employee"),
        R("6", "Self-employed in own not incorporated business"), R("7", "Self-employed in own incorporated business"),
        R("8", "Working without pay in family business or farm"), R("9", "Unemployed and last worked 5 years ago"))));
    colInfo.put("DECADE",
        new Triple<>("decade_of_entry_in_us", ColumnType.STRING,
            new ReplaceFn(//
                R("b", "born in US"), R("1", "Before 1950"), R("2", "1950 - 1959"), R("3", "1960 - 1969"),
                R("4", "1970 - 1979"), R("5", "1980 - 1989"), R("6", "1990 - 1999"), R("7", "2000 - 2009"))));
    colInfo.put("DIVISION",
        new Triple<>("division_code", ColumnType.STRING,
            new ReplaceFn(//
                R("0", "Puerto Rico"), R("1", "New England (Northeast region)"),
                R("2", "Middle Atlantic (Northeast region)"), R("3", "East North Central (Midwest region)"),
                R("4", "West North Central (Midwest region)"), R("5", "South Atlantic (South region)"),
                R("6", "East South Central (South region)"), R("7", "West South Central (South Region)"),
                R("8", "Mountain (West region)"), R("9", "Pacific (West region)"))));
    colInfo.put("DRIVESP", new Triple<>("number_of_vehicles_percentage", ColumnType.DOUBLE, new ReplaceFn(//
        R("", -1.), R("b", -1.), R("1", 1.), R("2", .5), R("3", .333), R("4", .25), R("5", .2), R("6", .143))));
    colInfo.put("ELEP", new Triple<>("electricity_cost_monthly", ColumnType.LONG, longSpecial(//
        R("bbb", -3L), R("001", -2L), R("002", -1L))));
    colInfo.put("ENG", new Triple<>("english_ability", ColumnType.STRING, new ReplaceFn(//
        R("b", "n/a"), R("1", "Very well"), R("2", "Well"), R("3", "Not well"), R("4", "Not at all"))));
    colInfo.put("ESP",
        new Triple<>("employment_status_parents", ColumnType.STRING,
            new ReplaceFn(//
                R("b", "n/a"), R("1", "Both parents in labor force"), R("2", "Father only in labor force"),
                R("3", "Mother only in labor force"), R("4", "Neither parent in labor force"),
                R("5", "Neither parent in labor force (one parent only)"),
                R("6", "Father not in labor force (one parent only)"),
                R("7", "Mother in the labor force (one parent only)"),
                R("8", "Mother not in labor force (one parent only)"))));
    colInfo.put("ESR",
        new Triple<>("employment_status_recode", ColumnType.STRING,
            new ReplaceFn(//
                R("b", "n/a"), R("1", "Civilian employed, at work"),
                R("2", "Civilian employed, with a job but not at work"), R("3", "Unemployed"),
                R("4", "Armed forces, at work"), R("5", "Armed forces, with a job but not at work"),
                R("6", "Not in labor force"))));
    colInfo.put("FACRP", new Triple<>("lot_size_allocation_flag", ColumnType.LONG, bool()));
    colInfo.put("FAGEP", new Triple<>("age_flag", ColumnType.LONG, bool()));
    colInfo.put("FAGSP", new Triple<>("saled_agriculture_flag", ColumnType.LONG, bool()));
    colInfo.put("FANCP", new Triple<>("ancestry_flag", ColumnType.LONG, bool()));
    colInfo.put("FBDSP", new Triple<>("bedrooms_flag", ColumnType.LONG, bool()));
    colInfo.put("FBLDP", new Triple<>("building_flag", ColumnType.LONG, bool()));
    colInfo.put("FBUSP", new Triple<>("business_or_medical_on_property_flag", ColumnType.LONG, bool()));
    colInfo.put("FCITP", new Triple<>("citizenship_flag", ColumnType.LONG, bool()));
    colInfo.put("FCONP", new Triple<>("condominium_fee_flag", ColumnType.LONG, bool()));
    colInfo.put("FCOWP", new Triple<>("class_of_worker_flag", ColumnType.LONG, bool()));
    colInfo.put("FELEP", new Triple<>("electricity_cost_flag", ColumnType.LONG, bool()));
    colInfo.put("FENGP", new Triple<>("english_ability_flag", ColumnType.LONG, bool()));
    colInfo.put("FER", new Triple<>("children_born_last_12_months", ColumnType.LONG, longSpecial(//
        R("b", -1L), R("1", 1L), R("2", 0L))));
    colInfo.put("FES",
        new Triple<>("family_type_and_employment_status", ColumnType.STRING,
            new ReplaceFn(//
                R("b", "n/a"), R("1", "Married-couple family: Husband and wife in LF"),
                R("2", "Married-couple family: Husband in labor force, wife not in LF"),
                R("3", "Married-couple family: Husband not in LF, wife in LF"),
                R("4", "Married-couple family: Neither husband nor wife in LF"),
                R("5", "Other family: Male householder, no wife present, in LF"),
                R("6", "Other family: Male householder, no wife present, not in LF"),
                R("7", "Other family: Female householder, no husband present, in LF"),
                R("8", "Other family: Female householder, no husband present, not in LF"))));
    colInfo.put("FESRP", new Triple<>("employment_status_recode_flag", ColumnType.LONG, bool()));
    colInfo.put("FFERP", new Triple<>("children_born_last_12_months_flag", ColumnType.LONG, bool()));
    colInfo.put("FFSP", new Triple<>("yearly_food_stamp_flag", ColumnType.LONG, bool()));
    colInfo.put("FFULP", new Triple<>("fuel_cost_yearly_flag", ColumnType.LONG, bool()));
    colInfo.put("FGASP", new Triple<>("gas_monthly_flag", ColumnType.LONG, bool()));
    colInfo.put("FGCLP", new Triple<>("grandchildren_living_in_house_flag", ColumnType.LONG, bool()));
    colInfo.put("FGCMP", new Triple<>("months_responsible_for_grandchildren_flag", ColumnType.LONG, bool()));
    colInfo.put("FGCRP", new Triple<>("responsible_for_grandchildren_flag", ColumnType.LONG, bool()));
    colInfo.put("FHFLP", new Triple<>("house_fuel_flag", ColumnType.LONG, bool()));
    colInfo.put("FHISP", new Triple<>("detailed_hispanic_origin_flag", ColumnType.LONG, bool()));
    colInfo.put("FINCP", new Triple<>("family_income_12_months", ColumnType.LONG, longSpecial(//
        R("bbbbbbbb", 0L))));
    colInfo.put("FINDP", new Triple<>("industry_flag", ColumnType.LONG, bool()));
    colInfo.put("FINSP", new Triple<>("fire_hazard_flood_insurance_yearly_flag", ColumnType.LONG, bool()));
    colInfo.put("FINTP", new Triple<>("interest_dividend_net_rental_income_flag", ColumnType.LONG, bool()));
    colInfo.put("FJWDP", new Triple<>("time_departure_to_work_flag", ColumnType.LONG, bool()));
    colInfo.put("FJWMNP", new Triple<>("travel_time_to_work_flag", ColumnType.LONG, bool()));
    colInfo.put("FJWRIP", new Triple<>("vehicle_occupancy_flag", ColumnType.LONG, bool()));
    colInfo.put("FJWTRP", new Triple<>("transportation_to_work_flag", ColumnType.LONG, bool()));
    colInfo.put("FKITP", new Triple<>("complete_kitchen_flag", ColumnType.LONG, bool()));
    colInfo.put("FLANP", new Triple<>("language_at_home_flag", ColumnType.LONG, bool()));
    colInfo.put("FLANXP", new Triple<>("language_other_than_english_flag", ColumnType.LONG, bool()));
    colInfo.put("FMARP", new Triple<>("marital_status_flag", ColumnType.LONG, bool()));
    colInfo.put("FMHP", new Triple<>("mobile_home_cost_yearly_flag", ColumnType.LONG, bool()));
    colInfo.put("FMIGP", new Triple<>("mobility_status_flag", ColumnType.LONG, bool()));
    colInfo.put("FMIGSP", new Triple<>("migration_state_flag", ColumnType.LONG, bool()));
    colInfo.put("FMILPP", new Triple<>("military_periods_flag", ColumnType.LONG, bool()));
    colInfo.put("FMILSP", new Triple<>("military_service_flag", ColumnType.LONG, bool()));
    colInfo.put("FMRGIP", new Triple<>("first_mortgage_includes_insurance_flag", ColumnType.LONG, bool()));
    colInfo.put("FMRGP", new Triple<>("first_mortgage_payment_flag", ColumnType.LONG, bool()));
    colInfo.put("FMRGTP", new Triple<>("first_mortgage_includes_taxes_flag", ColumnType.LONG, bool()));
    colInfo.put("FMRGXP", new Triple<>("first_mortgage_status_flag", ColumnType.LONG, bool()));
    colInfo.put("FMVP", new Triple<>("when_moved_in_flag", ColumnType.LONG, bool()));
    colInfo.put("FOCCP", new Triple<>("occupation_flag", ColumnType.LONG, bool()));
    colInfo.put("FOIP", new Triple<>("other_income_flag", ColumnType.LONG, bool()));
    colInfo.put("FPAP", new Triple<>("public_assistance_income_flag", ColumnType.LONG, bool()));
    colInfo.put("FPARC",
        new Triple<>("family_presence_and_age_of_related_children", ColumnType.STRING,
            new ReplaceFn(//
                R("b", "n/a"), R("1", "With related children under 5 years only"),
                R("2", "With related children 5 to 17 years only"),
                R("3", "With related children under 5 years and 5 to 17 years"), R("4", "No related children"))));
    colInfo.put("FPLMP", new Triple<>("complete_plumbing_flag", ColumnType.LONG, bool()));
    colInfo.put("FPOBP", new Triple<>("place_of_birth_flag", ColumnType.LONG, bool()));
    colInfo.put("FPOWSP", new Triple<>("palce_of_work_state_flag", ColumnType.LONG, bool()));
    colInfo.put("FRACP", new Triple<>("detailed_race_flag", ColumnType.LONG, bool()));
    colInfo.put("FRELP", new Triple<>("relationship_flag", ColumnType.LONG, bool()));
    colInfo.put("FRETP", new Triple<>("retirement_income_flag", ColumnType.LONG, bool()));
    colInfo.put("FRMSP", new Triple<>("rooms_allocation_flag", ColumnType.LONG, bool()));
    colInfo.put("FRNTMP", new Triple<>("rent_includes_meals_flag", ColumnType.LONG, bool()));
    colInfo.put("FRNTP", new Triple<>("rent_monthly_flag", ColumnType.LONG, bool()));
    colInfo.put("FS", new Triple<>("yearly_food_stamp", ColumnType.LONG, longSpecial(//
        R("b", -1L), R("1", 1L), R("2", 0L))));
    colInfo.put("FSCHGP", new Triple<>("grade_attending_flag", ColumnType.LONG, bool()));
    colInfo.put("FSCHLP", new Triple<>("highest_education_flag", ColumnType.LONG, bool()));
    colInfo.put("FSCHP", new Triple<>("school_enrollment_flag", ColumnType.LONG, bool()));
    colInfo.put("FSEMP", new Triple<>("self_employment_income_flag", ColumnType.LONG, bool()));
    colInfo.put("FSEXP", new Triple<>("sex_flag", ColumnType.LONG, bool()));
    colInfo.put("FSMP", new Triple<>("second_junior_mortgage_payment_monthly_flag", ColumnType.LONG, bool()));
    colInfo.put("FSMXHP", new Triple<>("home_equity_loan_flag", ColumnType.LONG, bool()));
    colInfo.put("FSMXSP", new Triple<>("second_mortgage_flag", ColumnType.LONG, bool()));
    colInfo.put("FSSIP", new Triple<>("supplementary_security_income_flag", ColumnType.LONG, bool()));
    colInfo.put("FSSP", new Triple<>("social_security_income_flag", ColumnType.LONG, bool()));
    colInfo.put("FTAXP", new Triple<>("taxes_on_poperty_flag", ColumnType.LONG, bool()));
    colInfo.put("FTELP", new Triple<>("telephone_in_house_flag", ColumnType.LONG, bool()));
    colInfo.put("FTENP", new Triple<>("tenure_allocation_flag", ColumnType.LONG, bool()));
    colInfo.put("FULP", new Triple<>("fuel_cost_yearly", ColumnType.LONG, longSpecial(//
        R("bbbb", -3L), R("0001", -2L), R("0002", -1L))));
    colInfo.put("FVACSP", new Triple<>("vacancy_status_flag", ColumnType.LONG, bool()));
    colInfo.put("FVALP", new Triple<>("property_value_flag", ColumnType.LONG, bool()));
    colInfo.put("FVEHP", new Triple<>("vehicles_available_flag", ColumnType.LONG, bool()));
    colInfo.put("FWAGP", new Triple<>("wages_salary_income_flag", ColumnType.LONG, bool()));
    colInfo.put("FWATP", new Triple<>("water_cost_yearly_flag", ColumnType.LONG, bool()));
    colInfo.put("FWKHP", new Triple<>("hours_worked_per_week_flag", ColumnType.LONG, bool()));
    colInfo.put("FWKLP", new Triple<>("last_worked_flag", ColumnType.LONG, bool()));
    colInfo.put("FWKWP", new Triple<>("weeks_worked_flag", ColumnType.LONG, bool()));
    colInfo.put("FYBLP", new Triple<>("year_building_built_flag", ColumnType.LONG, bool()));
    colInfo.put("FYOEP", new Triple<>("year_of_entry_flag", ColumnType.LONG, bool()));
    colInfo.put("GASP", new Triple<>("gas_monthly", ColumnType.LONG, longSpecial( //
        R("bbb", -4L), R("001", -3L), R("002", -2L), R("003", -1L))));
    colInfo.put("GCL", new Triple<>("grandchildren_living_in_house", ColumnType.LONG, longSpecial(//
        R("b", -1L), R("1", 1L), R("2", 0L))));
    colInfo.put("GCM",
        new Triple<>("months_responsible_for_grandchildren", ColumnType.STRING,
            new ReplaceFn(//
                R("b", "n/a"), R("1", "Less than 6 months"), R("2", "6 to 11 months"), R("3", "1 to 2 years"),
                R("4", "3 to 4 years"), R("5", "5 or more years"))));
    colInfo.put("GCR", new Triple<>("responsible_for_grandchildren", ColumnType.LONG, longSpecial(//
        R("b", "n/a"), R("1", 1L), R("2", 0L))));
    colInfo.put("GRNTP", new Triple<>("gross_rent", ColumnType.LONG, longSpecial(//
        R("bbbb", -1L))));
    colInfo.put("GRPIP", new Triple<>("gross_rent_percentage_of_income", ColumnType.LONG, longSpecial(//
        R("bbb", -1L))));
    colInfo.put("HFL",
        new Triple<>("house_heating_fuel", ColumnType.STRING,
            new ReplaceFn(//
                R("b", "N/A (GQ/vacant)"), R("1", "Utility gas"), R("2", "Bottled, tank, or LP gas"),
                R("3", "Electricity"), R("4", "Fuel oil, kerosene, etc."), R("5", "Coal or coke"), R("6", "Wood"),
                R("7", "Solar energy"), R("8", "Other fuel"), R("9", "No fuel used"))));
    colInfo.put("HHL",
        new Triple<>("household_language", ColumnType.STRING,
            new ReplaceFn(//
                R("b", "n/a"), R("1", "English only"), R("2", "Spanish"), R("3", "Other Indo-European languages"),
                R("4", "Asian and Pacific Island languages"), R("5", "Other language"))));
    colInfo.put("HHT",
        new Triple<>("household_family_type", ColumnType.STRING,
            new ReplaceFn(//
                R("b", "n/a"), R("1", "Married-couple family household"), R("2", "Male householder, no wife present"),
                R("3", "Female householder, no husband present"), R("4", "Living alone (male)"),
                R("5", "Not living alone (male)"), R("6", "Living alone (female)"), R("7", "Living alone (female)"))));
    colInfo.put("HINCP", new Triple<>("household_income_last_12_months", ColumnType.LONG, longSpecial(//
        R("bbbbbbbb", 0L))));
    colInfo.put("HISP",
        new Triple<>("detailed_hispanic_origin_recoded", ColumnType.STRING,
            new ReplaceFn(//
                R("01", "Not Spanish/Hispanic/Latino"), R("02", "Mexican"), R("03", "Puerto Rican"), R("04", "Cuban"),
                R("05", "Dominican"), R("06", "Costa Rican"), R("07", "Guatemalan"), R("08", "Honduran"),
                R("09", "Nicaraguan"), R("10", "Panamanian"), R("11", "Salvadoran"), R("12", "Other Central American"),
                R("13", "Argentinean"), R("14", "Bolivian"), R("15", "Chilean"), R("16", "Colombian"),
                R("17", "Ecuadorian"), R("18", "Paraguayan"), R("19", "Peruvian"), R("20", "Uruguayan"),
                R("21", "Venezuelan"), R("22", "Other South American"), R("23", "Spaniard"),
                R("24", "All Other Spanish/Hispanic/Latino"))));
    colInfo.put("HUGCL", new Triple<>("grandchildren_in_housing_unit", ColumnType.LONG, longSpecial(//
        R("b", -1L), R("1", 1L), R("2", 0L))));
    colInfo.put("HUPAC",
        new Triple<>("houshold_age_of_children", ColumnType.STRING,
            new ReplaceFn(//
                R("b", "n/a"), R("1", "With children under 6 years only"), R("2", "With children 6 to 17 years only"),
                R("3", "With children under 6 years and 6 to 17 years"), R("4", "No children"))));
    colInfo.put("HUPAOC", new Triple<>("houshold_age_of_own_children", ColumnType.STRING,

    new ReplaceFn(//
        R("b", "n/a"), R("1", "With own children under 6 years only"), R("2", "With own children 6 to 17 years only"),
        R("3", "With own children under 6 years and 6 to 17 years"), R("4", "No children"))));
    colInfo.put("HUPARC",
        new Triple<>("houshold_age_of_related_children", ColumnType.STRING,
            new ReplaceFn(//
                R("b", "n/a"), R("1", "With related children under 6 years only"),
                R("2", "With related children 6 to 17 years only"),
                R("3", "With related children under 6 years and 6 to 17 years"), R("4", "No children"))));
    colInfo
        .put("INDP",
            new Triple<>("industry_recode", ColumnType.STRING,
                new ReplaceFn(//
                    R("bbbb",
                        "N/A (less than 16 years old/NILF who last worked more than 5 years ago or never worked)"),
                    R("0170", "AGR-CROP PRODUCTION"), R("0180", "AGR-ANIMAL PRODUCTION"),
                    R("0190", "AGR-FORESTRY EXCEPT LOGGING"), R("0270", "AGR-LOGGING"),
                    R("0280", "AGR-FISHING, HUNTING, AND TRAPPING"),
                    R("0290", "AGR-SUPPORT ACTIVITIES FOR AGRICULTURE AND FORESTRY"),
                    R("0370", "EXT-OIL AND GAS EXTRACTION"), R("0380", "EXT-COAL MINING"),
                    R("0390", "EXT-METAL ORE MINING"), R("0470", "EXT-NONMETALLIC MINERAL MINING AND QUARRYING"),
                    R("0480", "EXT-NOT SPECIFIED TYPE OF MINING"), R("0490", "EXT-SUPPORT ACTIVITIES FOR MINING"),
                    R("0570", "UTL-ELECTRIC POWER GENERATION, TRANSMISSION AND DISTRIBUTION"), R("0580",
                        "UTL-NATURAL GAS DISTRIBUTION"),
                    R("0590", "UTL-ELECTRIC AND GAS, AND OTHER COMBINATIONS"),
                    R("0670", "UTL-WATER, STEAM, AIR CONDITIONING, AND IRRIGATION SYSTEMS"),
                    R("0680", "UTL-SEWAGE TREATMENT FACILITIES"), R("0690", "UTL-NOT SPECIFIED UTILITIES"),
                    R("0770", "CON-CONSTRUCTION, INCL CLEANING DURING AND IMM AFTER"),
                    R("1070", "MFG-ANIMAL FOOD, GRAIN AND OILSEED MILLING"),
                    R("1080", "MFG-SUGAR AND CONFECTIONERY PRODUCTS"),
                    R("1090", "MFG-FRUIT AND VEGETABLE PRESERVING AND SPECIALTY FOODS"),
                    R("1170", "MFG-DAIRY PRODUCTS"), R("1180", "MFG-ANIMAL SLAUGHTERING AND PROCESSING"),
                    R("1190", "MFG-RETAIL BAKERIES"), R("1270", "MFG-BAKERIES, EXCEPT RETAIL"),
                    R("1280", "MFG-SEAFOOD AND OTHER MISCELLANEOUS FOODS, N.E.C."),
                    R("1290", "MFG-NOT SPECIFIED FOOD INDUSTRIES"), R("1370", "MFG-BEVERAGE"), R("1390", "MFG-TOBACCO"),
                    R("1470", "MFG-FIBER, YARN, AND THREAD MILLS"),
                    R("1480", "MFG-FABRIC MILLS, EXCEPT KNITTING MILLS"),
                    R("1490", "MFG-TEXTILE AND FABRIC FINISHING AND FABRIC COATING MILLS"), R("1570",
                        "MFG-CARPET AND RUG MILLS"),
                    R("1590", "MFG-TEXTILE PRODUCT MILLS, EXCEPT CARPET AND RUG"),
                    R("1670", "MFG-KNITTING FABRIC MILLS, AND APPAREL KNITTING MILLS"),
                    R("1680", "MFG-CUT AND SEW APPAREL"), R("1690", "MFG-APPAREL ACCESSORIES AND OTHER APPAREL"),
                    R("1770", "MFG-FOOTWEAR"),
                    R("1790", "MFG-LEATHER TANNING AND FINISHING AND OTHER ALLIED PRODUCTS MANUFACTURING"),
                    R("1870", "MFG-PULP, PAPER, AND PAPERBOARD MILLS"),
                    R("1880", "MFG-PAPERBOARD CONTAINERS AND BOXES"),
                    R("1890", "MFG-MISCELLANEOUS PAPER AND PULP PRODUCTS"),
                    R("1990", "MFG-PRINTING AND RELATED SUPPORT ACTIVITIES"), R("2070", "MFG-PETROLEUM REFINING"),
                    R("2090", "MFG-MISCELLANEOUS PETROLEUM AND COAL PRODUCTS"),
                    R("2170", "MFG-RESIN, SYNTHETIC RUBBER, AND FIBERS AND FILAMENTS"),
                    R("2180", "MFG-AGRICULTURAL CHEMICALS"), R("2190", "MFG-PHARMACEUTICALS AND MEDICINES"),
                    R("2270", "MFG-PAINT, COATING, AND ADHESIVES"),
                    R("2280", "MFG-SOAP, CLEANING COMPOUND, AND COSMETICS"),
                    R("2290", "MFG-INDUSTRIAL AND MISCELLANEOUS CHEMICALS"), R("2370", "MFG-PLASTICS PRODUCTS"),
                    R("2380", "MFG-TIRES"), R("2390", "MFG-RUBBER PRODUCTS, EXCEPT TIRES"),
                    R("2470", "MFG-POTTERY, CERAMICS, AND PLUMBING FIXTURE MANUFACTURING"),
                    R("2480", "MFG-STRUCTURAL CLAY PRODUCTS"), R("2490", "MFG-GLASS AND GLASS PRODUCTS"),
                    R("2570", "MFG-CEMENT, CONCRETE, LIME, AND GYPSUM PRODUCTS"), R("2590",
                        "MFG-MISCELLANEOUS NONMETALLIC MINERAL PRODUCTS"),
        R("2670", "MFG-IRON AND STEEL MILLS AND STEEL PRODUCTS"), R("2680", "MFG-ALUMINUM PRODUCTION AND PROCESSING"),
        R("2690", "MFG-NONFERROUS METAL, EXCEPT ALUMINUM, PRODUCTION AND PROCESSING"), R("2770", "MFG-FOUNDRIES"),
        R("2780", "MFG-METAL FORGINGS AND STAMPINGS"), R("2790", "MFG-CUTLERY AND HAND TOOLS"),
        R("2870", "MFG-STRUCTURAL METALS, AND BOILER, TANK, AND SHIPPING CONTAINERS"),
        R("2880", "MFG-MACHINE SHOPS; TURNED PRODUCTS; SCREWS, NUTS AND BOLTS"),
        R("2890", "MFG-COATING, ENGRAVING, HEAT TREATING AND ALLIED ACTIVITIES"), R("2970", "MFG-ORDNANCE"),
        R("2980", "MFG-MISCELLANEOUS FABRICATED METAL PRODUCTS"), R("2990", "MFG-NOT SPECIFIED METAL INDUSTRIES"),
        R("3070", "MFG-AGRICULTURAL IMPLEMENTS"),
        R("3080", "MFG-CONSTRUCTION, AND MINING AND OIL AND GAS FIELD MACHINERY"),
        R("3090", "MFG-COMMERCIAL AND SERVICE INDUSTRY MACHINERY"), R("3170", "MFG-METALWORKING MACHINERY"),
        R("3180", "MFG-ENGINES, TURBINES, AND POWER TRANSMISSION EQUIPMENT"), R("3190", "MFG-MACHINERY, N.E.C."),
        R("3290", "MFG-NOT SPECIFIED MACHINERY"), R("3360", "MFG-COMPUTER AND PERIPHERAL EQUIPMENT"),
        R("3370", "MFG-COMMUNICATIONS, AND AUDIO AND VIDEO EQUIPMENT"),
        R("3380", "MFG-NAVIGATIONAL, MEASURING, ELECTROMEDICAL, AND CONTROL INSTRUMENTS"),
        R("3390", "MFG-ELECTRONIC COMPONENTS AND PRODUCTS, N.E.C."), R("3470", "MFG-HOUSEHOLD APPLIANCES"),
        R("3490",
            "MFG-ELECTRIC LIGHTING AND ELECTRICAL EQUIPMENT MANUFACTURING, AND OTHER ELECTRICAL COMPONENT MANUFACTURING, N.E.C."),
        R("3570", "MFG-MOTOR VEHICLES AND MOTOR VEHICLE EQUIPMENT"), R("3580", "MFG-AIRCRAFT AND PARTS"),
        R("3590", "MFG-AEROSPACE PRODUCTS AND PARTS"), R("3670", "MFG-RAILROAD ROLLING STOCK"),
        R("3680", "MFG-SHIP AND BOAT BUILDING"), R("3690", "MFG-OTHER TRANSPORTATION EQUIPMENT"),
        R("3770", "MFG-SAWMILLS AND WOOD PRESERVATION"), R("3780", "MFG-VENEER, PLYWOOD, AND ENGINEERED WOOD PRODUCTS"),
        R("3790", "MFG-PREFABRICATED WOOD BUILDINGS AND MOBILE HOMES"), R("3870", "MFG-MISCELLANEOUS WOOD PRODUCTS"),
        R("3890", "MFG-FURNITURE AND RELATED PRODUCTS"), R("3960", "MFG-MEDICAL EQUIPMENT AND SUPPLIES"),
        R("3970", "MFG-SPORTING AND ATHLETIC GOODS, AND DOLL, TOY, AND GAME MANUFACTURING"),
        R("3980", "MFG-MISCELLANEOUS MANUFACTURING, N.E.C."), R("3990", "MFG-NOT SPECIFIED MANUFACTURING INDUSTRIES"),
        R("4070", "WHL-MOTOR VEHICLES, PARTS AND SUPPLIES MERCHANT WHOLESALERS"),
        R("4080", "WHL-FURNITURE AND HOME FURNISHING MERCHANT WHOLESALERS"),
        R("4090", "WHL-LUMBER AND OTHER CONSTRUCTION MATERIALS MERCHANT WHOLESALERS"),
        R("4170", "WHL-PROFESSIONAL AND COMMERCIAL EQUIPMENT AND SUPPLIES MERCHANT WHOLESALERS"),
        R("4180", "WHL-METALS AND MINERALS, EXCEPT PETROLEUM, MERCHANT WHOLESALERS"),
        R("4190", "WHL-ELECTRICAL AND ELECTRONIC GOODS MERCHANT WHOLESALERS"),
        R("4260", "WHL-HARDWARE, PLUMBING AND HEATING EQUIPMENT, AND SUPPLIES MERCHANT WHOLESALERS"),
        R("4270", "WHL-MACHINERY, EQUIPMENT, AND SUPPLIES MERCHANT WHOLESALERS"),
        R("4280", "WHL-RECYCLABLE MATERIAL MERCHANT WHOLESALERS"),
        R("4290", "WHL-MISCELLANEOUS DURABLE GOODS MERCHANT WHOLESALERS"),
        R("4370", "WHL-PAPER AND PAPER PRODUCTS MERCHANT WHOLESALERS"),
        R("4380", "WHL-DRUGS, SUNDRIES, AND CHEMICAL AND ALLIED PRODUCTS MERCHANT WHOLESALERS"),
        R("4390", "WHL-APPAREL, FABRICS, AND NOTIONS MERCHANT WHOLESALERS"),
        R("4470", "WHL-GROCERIES AND RELATED PRODUCTS MERCHANT WHOLESALERS"),
        R("4480", "WHL-FARM PRODUCT RAW MATERIALS MERCHANT WHOLESALERS"),
        R("4490", "WHL-PETROLEUM AND PETROLEUM PRODUCTS MERCHANT WHOLESALERS"),
        R("4560", "WHL-ALCOHOLIC BEVERAGES MERCHANT WHOLESALERS"), R("4570", "WHL-FARM SUPPLIES MERCHANT WHOLESALERS"),
        R("4580", "WHL-MISCELLANEOUS NONDURABLE GOODS MERCHANT WHOLESALERS"),
        R("4585", "WHL-ELECTRONIC MARKETS AND AGENTS AND BROKERS"), R("4590", "WHL-NOT SPECIFIED WHOLESALE TRADE"),
        R("4670", "RET-AUTOMOBILE DEALERS"), R("4680", "RET-OTHER MOTOR VEHICLE DEALERS"),
        R("4690", "RET-AUTO PARTS, ACCESSORIES, AND TIRE STORES"),
        R("4770", "RET-FURNITURE AND HOME FURNISHINGS STORES"), R("4780", "RET-HOUSEHOLD APPLIANCE STORES"),
        R("4790", "RET-RADIO, TV, AND COMPUTER STORES"), R("4870", "RET-BUILDING MATERIAL AND SUPPLIES DEALERS"),
        R("4880", "RET-HARDWARE STORES"), R("4890", "RET-LAWN AND GARDEN EQUIPMENT AND SUPPLIES STORES"),
        R("4970", "RET-GROCERY STORES"), R("4980", "RET-SPECIALTY FOOD STORES"),
        R("4990", "RET-BEER, WINE, AND LIQUOR STORES"), R("5070", "RET-PHARMACIES AND DRUG STORES"),
        R("5080", "RET-HEALTH AND PERSONAL CARE, EXCEPT DRUG, STORES"), R("5090", "RET-GASOLINE STATIONS"),
        R("5170", "RET-CLOTHING STORES"), R("5180", "RET-SHOE STORES"),
        R("5190", "RET-JEWELRY, LUGGAGE, AND LEATHER GOODS STORES"),
        R("5270", "RET-SPORTING GOODS, CAMERA, AND HOBBY AND TOY STORES"),
        R("5280", "RET-SEWING, NEEDLEWORK AND PIECE GOODS STORES"), R("5290", "RET-MUSIC STORES"),
        R("5370", "RET-BOOK STORES AND NEWS DEALERS"), R("5380", "RET-DEPARTMENT AND DISCOUNT STORES"),
        R("5390", "RET-MISCELLANEOUS GENERAL MERCHANDISE STORES"), R("5470", "RET-FLORISTS"),
        R("5480", "RET-OFFICE SUPPLIES AND STATIONERY STORES"), R("5490", "RET-USED MERCHANDISE STORES"),
        R("5570", "RET-GIFT, NOVELTY, AND SOUVENIR SHOPS"), R("5580", "RET-MISCELLANEOUS RETAIL STORES"),
        R("5590", "RET-ELECTRONIC SHOPPING"), R("5591", "RET-ELECTRONIC AUCTIONS"), R("5592", "RET-MAIL-ORDER HOUSES"),
        R("5670", "RET-VENDING MACHINE OPERATORS"), R("5680", "RET-FUEL DEALERS"),
        R("5690", "RET-OTHER DIRECT SELLING ESTABLISHMENTS"), R("5790", "RET-NOT SPECIFIED RETAIL TRADE"),
        R("6070", "TRN-AIR TRANSPORTATION"), R("6080", "TRN-RAIL TRANSPORTATION"),
        R("6090", "TRN-WATER TRANSPORTATION"), R("6170", "TRN-TRUCK TRANSPORTATION"),
        R("6180", "TRN-BUS SERVICE AND URBAN TRANSIT"), R("6190", "TRN-TAXI AND LIMOUSINE SERVICE"),
        R("6270", "TRN-PIPELINE TRANSPORTATION"), R("6280", "TRN-SCENIC AND SIGHTSEEING TRANSPORTATION"),
        R("6290", "TRN-SERVICES INCIDENTAL TO TRANSPORTATION"), R("6370", "TRN-POSTAL SERVICE"),
        R("6380", "TRN-COURIERS AND MESSENGERS"), R("6390", "TRN-WAREHOUSING AND STORAGE"),
        R("6470", "INF-NEWSPAPER PUBLISHERS"), R("6480", "INF-PERIODICAL, BOOK, AND DIRECTORY PUBLISHERS"),
        R("6490", "INF-SOFTWARE PUBLISHERS"), R("6570", "INF-MOTION PICTURE AND VIDEO INDUSTRIES"),
        R("6590", "INF-SOUND RECORDING INDUSTRIES"),
        R("6670", "INF-BROADCASTING, INCLUDING INTERNET PUBLISHING AND WEB SEARCH PORTALS"),
        R("6680", "INF-WIRED TELECOMMUNICATIONS CARRIERS"), R("6690", "INF-OTHER TELECOMMUNICATION SERVICES"),
        R("6695", "INF-DATA PROCESSING, HOSTING, AND RELATED SERVICES"), R("6770", "INF-LIBRARIES AND ARCHIVES"),
        R("6780",
            "INF-OTHER INFORMATION SERVICES, EXC LIBRARIES AND ARCHIVES, AND INTERNET PUBLISHING AND BROADCASTING AND WEB SEARCH PORTALS"),
        R("6870", "FIN-BANKING AND RELATED ACTIVITIES"), R("6880", "FIN-SAVINGS INSTITUTIONS, INCLUDING CREDIT UNIONS"),
        R("6890", "FIN-NON-DEPOSITORY CREDIT AND RELATED ACTIVITIES"),
        R("6970", "FIN-SECURITIES, COMMODITIES, FUNDS, TRUSTS, AND OTHER FINANCIAL INVESTMENTS"),
        R("6990", "FIN-INSURANCE CARRIERS AND RELATED ACTIVITIES"), R("7070", "FIN-REAL ESTATE"),
        R("7080", "FIN-AUTOMOTIVE EQUIPMENT RENTAL AND LEASING"), R("7170", "FIN-VIDEO TAPE AND DISK RENTAL"),
        R("7180", "FIN-OTHER CONSUMER GOODS RENTAL"),
        R("7190", "FIN-COMMERCIAL, INDUSTRIAL, AND OTHER INTANGIBLE ASSETS RENTAL AND LEASING"),
        R("7270", "PRF-LEGAL SERVICES"), R("7280", "PRF-ACCOUNTING, TAX PREPARATION, BOOKKEEPING AND PAYROLL SERVICES"),
        R("7290", "PRF-ARCHITECTURAL, ENGINEERING, AND RELATED SERVICES"), R("7370", "PRF-SPECIALIZED DESIGN SERVICES"),
        R("7380", "PRF-COMPUTER SYSTEMS DESIGN AND RELATED SERVICES"),
        R("7390", "PRF-MANAGEMENT, SCIENTIFIC, AND TECHNICAL CONSULTING SERVICES"),
        R("7460", "PRF-SCIENTIFIC RESEARCH AND DEVELOPMENT SERVICES"),
        R("7470", "PRF-ADVERTISING AND RELATED SERVICES"), R("7480", "PRF-VETERINARY SERVICES"),
        R("7490", "PRF-OTHER PROFESSIONAL, SCIENTIFIC, AND TECHNICAL SERVICES"),
        R("7570", "PRF-MANAGEMENT OF COMPANIES AND ENTERPRISES"), R("7580", "PRF-EMPLOYMENT SERVICES"),
        R("7590", "PRF-BUSINESS SUPPORT SERVICES"), R("7670", "PRF-TRAVEL ARRANGEMENTS AND RESERVATION SERVICES"),
        R("7680", "PRF-INVESTIGATION AND SECURITY SERVICES"),
        R("7690", "PRF-SERVICES TO BUILDINGS AND DWELLINGS, EX CONSTR CLN"), R("7770", "PRF-LANDSCAPING SERVICES"),
        R("7780", "PRF-OTHER ADMINISTRATIVE, AND OTHER SUPPORT SERVICES"),
        R("7790", "PRF-WASTE MANAGEMENT AND REMEDIATION SERVICES"), R("7860", "EDU-ELEMENTARY AND SECONDARY SCHOOLS"),
        R("7870", "EDU-COLLEGES AND UNIVERSITIES, INCLUDING JUNIOR COLLEGES"),
        R("7880", "EDU-BUSINESS, TECHNICAL, AND TRADE SCHOOLS AND TRAINING"),
        R("7890", "EDU-OTHER SCHOOLS AND INSTRUCTION, AND EDUCATIONAL SUPPORT SERVICES"),
        R("7970", "MED-OFFICES OF PHYSICIANS"), R("7980", "MED-OFFICES OF DENTISTS"),
        R("7990", "MED-OFFICE OF CHIROPRACTORS"), R("8070", "MED-OFFICES OF OPTOMETRISTS"),
        R("8080", "MED-OFFICES OF OTHER HEALTH PRACTITIONERS"), R("8090", "MED-OUTPATIENT CARE CENTERS"),
        R("8170", "MED-HOME HEALTH CARE SERVICES"), R("8180", "MED-OTHER HEALTH CARE SERVICES"),
        R("8190", "MED-HOSPITALS"), R("8270", "MED-NURSING CARE FACILITIES"),
        R("8290", "MED-RESIDENTIAL CARE FACILITIES, WITHOUT NURSING"), R("8370", "SCA-INDIVIDUAL AND FAMILY SERVICES"),
        R("8380", "SCA-COMMUNITY FOOD AND HOUSING, AND EMERGENCY SERVICES"),
        R("8390", "SCA-VOCATIONAL REHABILITATION SERVICES"), R("8470", "SCA-CHILD DAY CARE SERVICES"),
        R("8560", "ENT-INDEPENDENT ARTISTS, PERFORMING ARTS, SPECTATOR SPORTS AND RELATED INDUSTRIES"),
        R("8570", "ENT-MUSEUMS, ART GALLERIES, HISTORICAL SITES, AND SIMILAR INSTITUTIONS"),
        R("8580", "ENT-BOWLING CENTERS"), R("8590", "ENT-OTHER AMUSEMENT, GAMBLING, AND RECREATION INDUSTRIES"),
        R("8660", "ENT-TRAVELER ACCOMMODATION"),
        R("8670", "ENT-RECREATIONAL VEHICLE PARKS AND CAMPS, AND ROOMING AND BOARDING HOUSES"),
        R("8680", "ENT-RESTAURANTS AND OTHER FOOD SERVICES"), R("8690", "ENT-DRINKING PLACES, ALCOHOLIC BEVERAGES"),
        R("8770", "SRV-AUTOMOTIVE REPAIR AND MAINTENANCE"), R("8780", "SRV-CAR WASHES"),
        R("8790", "SRV-ELECTRONIC AND PRECISION EQUIPMENT REPAIR AND MAINTENANCE"),
        R("8870", "SRV-COMMERCIAL AND INDUSTRIAL MACHINERY AND EQUIPMENT REPAIR AND MAINTENANCE"),
        R("8880", "SRV-PERSONAL AND HOUSEHOLD GOODS REPAIR AND MAINTENANCE"), R("8970", "SRV-BARBER SHOPS"),
        R("8980", "SRV-BEAUTY SALONS"), R("8990", "SRV-NAIL SALONS AND OTHER PERSONAL CARE SERVICES"),
        R("9070", "SRV-DRYCLEANING AND LAUNDRY SERVICES"), R("9080", "SRV-FUNERAL HOMES, CEMETERIES AND CREMATORIES"),
        R("9090", "SRV-OTHER PERSONAL SERVICES"), R("9160", "SRV-RELIGIOUS ORGANIZATIONS"),
        R("9170", "SRV-CIVIC, SOCIAL, ADVOCACY ORGANIZATIONS, AND GRANTMAKING AND GIVING SERVICES"),
        R("9180", "SRV-LABOR UNIONS"), R("9190", "SRV-BUSINESS, PROFESSIONAL, POLITICAL AND SIMILAR ORGANIZATIONS"),
        R("9290", "SRV-PRIVATE HOUSEHOLDS"), R("9370", "ADM-EXECUTIVE OFFICES AND LEGISLATIVE BODIES"),
        R("9380", "ADM-PUBLIC FINANCE ACTIVITIES"), R("9390", "ADM-OTHER GENERAL GOVERNMENT AND SUPPORT"),
        R("9470", "ADM-JUSTICE, PUBLIC ORDER, AND SAFETY ACTIVITIES"),
        R("9480", "ADM-ADMINISTRATION OF HUMAN RESOURCE PROGRAMS"),
        R("9490", "ADM-ADMINISTRATION OF ENVIRONMENTAL QUALITY AND HOUSING PROGRAMS"),
        R("9570", "ADM-ADMINISTRATION OF ECONOMIC PROGRAMS AND SPACE RESEARCH"),
        R("9590", "ADM-NATIONAL SECURITY AND INTERNATIONAL AFFAIRS"), R("9670", "MIL-U.S. ARMY"),
        R("9680", "MIL-U.S. AIR FORCE"), R("9690", "MIL-U.S. NAVY"), R("9770", "MIL-U.S. MARINES"),
        R("9780", "MIL-U.S. COAST GUARD"), R("9790", "MIL-U.S. ARMED FORCES, BRANCH NOT SPECIFIED"),
        R("9870", "MIL-MILITARY RESERVES OR NATIONAL GUARD"),
        R("9920", "UNEMPLOYED AND LAST WORKED 5 YEARS AGO OR EARLIER OR NEVER WORKED"))));
    colInfo.put("INSP", new Triple<>("fire_hazard_flood_insurance_yearly", ColumnType.LONG, longSpecial(//
        R("bbbb", -1L))));
    colInfo.put("INTP", new Triple<>("interest_dividend_net_rental_income", ColumnType.LONG, longSpecial(//
        R("bbbbbb", 0L))));
    colInfo.put("JWAP",
        new Triple<>("time_arrival_at_work", ColumnType.STRING, new ReplaceFn(//
            R("bbb", "n/a"), R("001", "12:00 am"), R("002", "12:05 am"), R("003", "12:10 am"), R("004", "12:15 am"),
            R("005", "12:20 am"), R("006", "12:25 am"), R("007", "12:30 am"), R("008", "12:40 am"),
            R("009", "12:45 am"), R("010", "12:50 am"), R("011", "1:00 am"), R("012", "1:05 am"), R("013", "1:10 am"),
            R("014", "1:15 am"), R("015", "1:20 am"), R("016", "1:25 am"), R("017", "1:30 am"), R("018", "1:35 am"),
            R("019", "1:40 am"), R("020", "1:45 am"), R("021", "1:50 am"), R("022", "2:00 am"), R("023", "2:05 am"),
            R("024", "2:10 am"), R("025", "2:15 am"), R("026", "2:20 am"), R("027", "2:25 am"), R("028", "2:30 am"),
            R("029", "2:35 am"), R("030", "2:40 am"), R("031", "2:45 am"), R("032", "2:50 am"), R("033", "2:55 am"),
            R("034", "3:00 am"), R("035", "3:05 am"), R("036", "3:10 am"), R("037", "3:15 am"), R("038", "3:20 am"),
            R("039", "3:25 am"), R("040", "3:30 am"), R("041", "3:35 am"), R("042", "3:40 am"), R("043", "3:45 am"),
            R("044", "3:50 am"), R("045", "3:55 am"), R("046", "4:00 am"), R("047", "4:05 am"), R("048", "4:10 am"),
            R("049", "4:15 am"), R("050", "4:20 am"), R("051", "4:25 am"), R("052", "4:30 am"), R("053", "4:35 am"),
            R("054", "4:40 am"), R("055", "4:45 am"), R("056", "4:50 am"), R("057", "4:55 am"), R("058", "5:00 am"),
            R("059", "5:05 am"), R("060", "5:10 am"), R("061", "5:15 am"), R("062", "5:20 am"), R("063", "5:25 am"),
            R("064", "5:30 am"), R("065", "5:35 am"), R("066", "5:40 am"), R("067", "5:45 am"), R("068", "5:50 am"),
            R("069", "5:55 am"), R("070", "6:00 am"), R("071", "6:05 am"), R("072", "6:10 am"), R("073", "6:15 am"),
            R("074", "6:20 am"), R("075", "6:25 am"), R("076", "6:30 am"), R("077", "6:35 am"), R("078", "6:40 am"),
            R("079", "6:45 am"), R("080", "6:50 am"), R("081", "6:55 am"), R("082", "7:00 am"), R("083", "7:05 am"),
            R("084", "7:10 am"), R("085", "7:15 am"), R("086", "7:20 am"), R("087", "7:25 am"), R("088", "7:30 am"),
            R("089", "7:35 am"), R("090", "7:40 am"), R("091", "7:45 am"), R("092", "7:50 am"), R("093", "7:55 am"),
            R("094", "8:00 am"), R("095", "8:05 am"), R("096", "8:10 am"), R("097", "8:15 am"), R("098", "8:20 am"),
            R("099", "8:25 am"), R("100", "8:30 am"), R("101", "8:35 am"), R("102", "8:40 am"), R("103", "8:45 am"),
            R("104", "8:50 am"), R("105", "8:55 am"), R("106", "9:00 am"), R("107", "9:05 am"), R("108", "9:10 am"),
            R("109", "9:15 am"), R("110", "9:20 am"), R("111", "9:25 am"), R("112", "9:30 am"), R("113", "9:35 am"),
            R("114", "9:40 am"), R("115", "9:45 am"), R("116", "9:50 am"), R("117", "9:55 am"), R("118", "10:00 am"),
            R("119", "10:05 am"), R("120", "10:10 am"), R("121", "10:15 am"),
            R("122", "10:20 am"), R("123", "10:25 am"), R("124", "10:30 am"), R("125", "10:35 am"), R("126",
                "10:40 am"),
            R("127", "10:45 am"), R("128", "10:50 am"), R("129", "10:55 am"), R("130", "11:00 am"),
            R("131", "11:05 am"), R("132", "11:10 am"), R("133", "11:15 am"), R("134", "11:20 am"),
            R("135", "11:25 am"), R("136", "11:30 am"), R("137", "11:35 am"), R("138", "11:40 am"),
            R("139", "11:45 am"), R("140", "11:50 am"), R("141", "11:55 am"), R("142", "12:00 pm"),
            R("143", "12:05 pm"), R("144", "12:10 pm"), R("145", "12:15 pm"), R("146", "12:20 pm"),
            R("147", "12:25 pm"), R("148", "12:30 pm"), R("149", "12:35 pm"), R("150", "12:40 pm"),
            R("151", "12:45 pm"), R("152", "12:50 pm"), R("153", "12:55 pm"), R("154", "1:00 pm"), R("155", "1:05 pm"),
            R("156", "1:10 pm"), R("157", "1:15 pm"), R("158", "1:20 pm"), R("159", "1:25 pm"), R("160", "1:30 pm"),
            R("161", "1:35 pm"), R("162", "1:40 pm"), R("163", "1:45 pm"), R("164", "1:50 pm"), R("165", "1:55 pm"),
            R("166", "2:00 pm"), R("167", "2:05 pm"), R("168", "2:10 pm"), R("169", "2:15 pm"), R("170", "2:20 pm"),
            R("171", "2:25 pm"), R("172", "2:30 pm"), R("173", "2:35 pm"), R("174", "2:40 pm"), R("175", "2:45 pm"),
            R("176", "2:50 pm"), R("177", "2:55 pm"), R("178", "3:00 pm"), R("179", "3:05 pm"), R("180", "3:10 pm"),
            R("181", "3:15 pm"), R("182", "3:20 pm"), R("183", "3:25 pm"), R("184", "3:30 pm"), R("185", "3:35 pm"),
            R("186", "3:40 pm"), R("187", "3:45 pm"), R("188", "3:50 pm"), R("189", "3:55 pm"), R("190", "4:00 pm"),
            R("191", "4:05 pm"), R("192", "4:10 pm"), R("193", "4:15 pm"), R("194", "4:20 pm"), R("195", "4:25 pm"),
            R("196", "4:30 pm"), R("197", "4:35 pm"), R("198", "4:40 pm"), R("199", "4:45 pm"), R("200", "4:50 pm"),
            R("201", "4:55 pm"), R("202", "5:00 pm"), R("203", "5:05 pm"), R("204", "5:10 pm"), R("205", "5:15 pm"),
            R("206", "5:20 pm"), R("207", "5:25 pm"), R("208", "5:30 pm"), R("209", "5:35 pm"), R("210", "5:40 pm"),
            R("211", "5:45 pm"), R("212", "5:50 pm"), R("213", "5:55 pm"), R("214", "6:00 pm"), R("215", "6:05 pm"),
            R("216", "6:10 pm"), R("217", "6:15 pm"), R("218", "6:20 pm"), R("219", "6:25 pm"), R("220", "6:30 pm"),
            R("221", "6:35 pm"), R("222", "6:40 pm"), R("223", "6:45 pm"), R("224", "6:50 pm"), R("225", "6:55 pm"),
            R("226", "7:00 pm"), R("227", "7:05 pm"), R("228", "7:10 pm"), R("229", "7:15 pm"), R("230", "7:20 pm"),
            R("231", "7:25 pm"), R("232", "7:30 pm"), R("233", "7:35 pm"), R("234", "7:40 pm"), R("235", "7:45 pm"),
            R("236", "7:50 pm"), R("237", "7:55 pm"), R("238", "8:00 pm"), R("239", "8:05 pm"), R("240", "8:10 pm"),
            R("241", "8:15 pm"), R("242", "8:20 pm"), R("243", "8:25 pm"), R("244", "8:30 pm"), R("245", "8:35 pm"),
            R("246", "8:40 pm"), R("247", "8:45 pm"), R("248", "8:50 pm"), R("249", "8:55 pm"), R("250", "9:00 pm"),
            R("251", "9:05 pm"), R("252", "9:10 pm"), R("253", "9:15 pm"), R("254", "9:20 pm"), R("255", "9:25 pm"),
            R("256", "9:30 pm"), R("257", "9:35 pm"), R("258", "9:40 pm"), R("259", "9:45 pm"), R("260", "9:50 pm"),
            R("261", "9:55 pm"), R("262", "10:00 pm"), R("263", "10:05 pm"), R("264", "10:10 pm"), R("265", "10:15 pm"),
            R("266", "10:20 pm"), R("267", "10:25 pm"), R("268", "10:30 pm"), R("269", "10:35 pm"),
            R("270", "10:40 pm"), R("271", "10:45 pm"), R("272", "10:50 pm"), R("273", "10:55 pm"),
            R("274", "11:00 pm"), R("275", "11:05 pm"), R("276", "11:10 pm"), R("277", "11:15 pm"),
            R("278", "11:20 pm"), R("279", "11:25 pm"), R("280", "11:30 pm"), R("281", "11:35 pm"),
            R("282", "11:40 pm"), R("283", "11:45 pm"), R("284", "11:50 pm"), R("285", "11:55 pm"))));
    colInfo.put("JWDP", new Triple<>("time_departure_at_work", ColumnType.STRING, colInfo.get("JWAP").getRight()));
    colInfo.put("JWMNP", new Triple<>("travl_time_to_work_minutes", ColumnType.LONG, longSpecial(//
        R("bbb", 0L))));
    colInfo.put("JWRIP",
        new Triple<>("vehicle_occupancy", ColumnType.STRING,
            new ReplaceFn(//
                R("bb", "n/a"), R("01", "Drove alone"), R("02", "In 2-person carpool"), R("03", "In 3-person carpool"),
                R("04", "In 4-person carpool"), R("05", "In 5-person carpool"), R("06", "In 6-person carpool"),
                R("07", "In 7-person carpool"), R("08", "In 8-person carpool"), R("09", "In 9-person carpool"),
                R("10", "In 10-person or more carpool"))));
    colInfo.put("JWTR",
        new Triple<>("transportation_to_work", ColumnType.STRING,
            new ReplaceFn(//
                R("bb",
                    "N/A (not a worker--not in the labor force, including persons under 16 years; unemployed; employed, with a job but not at work; Armed Forces, with a job but not at work)"),
                R("01", "Car, truck, or van"), R("02", "Bus or trolley bus"),
                R("03", "Streetcar or trolley car (carro publico in Puerto Rico)"), R("04", "Subway or elevated"),
                R("05", "Railroad"), R("06", "Ferryboat"), R("07", "Taxicab"), R("08", "Motorcycle"),
                R("09", "Bicycle"), R("10", "Walked"), R("11", "Worked at home"), R("12", "Other method"))));
    colInfo.put("KIT", new Triple<>("complete_kitchen", ColumnType.LONG, longSpecial(//
        R("b", -1L), R("1", 1L), R("2", 0L))));
    colInfo.put("LANP",
        new Triple<>("language_at_home", ColumnType.STRING,
            new ReplaceFn(//
                R("bbb", "N/A (less than 5 years old/speaks only English)"), R("601", "Jamaican Creole"),
                R("607", "German"), R("608", "Pennsylvania Dutch"), R("609", "Yiddish"), R("610", "Dutch"),
                R("611", "Afrikaans"), R("614", "Swedish"), R("615", "Danish"), R("616", "Norwegian"),
                R("619", "Italian"), R("620", "French"), R("622", "Patois"), R("623", "French Creole"), R("624",
                    "Cajun"),
                R("625", "Spanish"), R("629", "Portuguese"), R("631", "Romanian"), R("635", "Irish Gaelic"),
                R("637", "Greek"), R("638", "Albanian"), R("639", "Russian"), R("641", "Ukrainian"), R("642", "Czech"),
                R("645", "Polish"), R("646", "Slovak"), R("647", "Bulgarian"), R("648", "Macedonian"),
                R("649", "Serbocroatian"), R("650", "Croatian"), R("651", "Serbian"), R("653", "Lithuanian"),
                R("654", "Lettish"), R("655", "Armenian"), R("656", "Persian"), R("657", "Pashto"), R("658", "Kurdish"),
                R("662", "India n.e.c."), R("663", "Hindi"), R("664", "Bengali"), R("665", "Panjabi"),
                R("666", "Marathi"), R("667", "Gujarati"), R("671", "Urdu"), R("674", "Nepali"),
                R("676", "Pakistan n.e.c."), R("677", "Sinhalese"), R("679", "Finnish"), R("682", "Hungarian"),
                R("691", "Turkish"), R("701", "Telugu"), R("702", "Kannada"), R("703", "Malayalam"), R("704", "Tamil"),
                R("708", "Chinese"), R("711", "Cantonese"), R("712", "Mandarin"), R("714", "Formosan"),
                R("717", "Burmese"), R("720", "Thai"), R("721", "Miao-yao, Mien"), R("722", "Hmong"),
                R("723", "Japanese"), R("724", "Korean"), R("725", "Laotian"), R("726", "Mon-Khmer, Cambodian"),
                R("728", "Vietnamese"), R("732", "Indonesian"), R("739", "Malay"), R("742", "Tagalog"),
                R("743", "Bisayan"), R("744", "Sebuano"), R("746", "Ilocano"), R("752", "Chamorro"), R("767", "Samoan"),
                R("768", "Tongan"), R("776", "Hawaiian"), R("777", "Arabic"), R("778", "Hebrew"), R("779", "Syriac"),
                R("780", "Amharic"), R("783", "Cushite"), R("791", "Swahili"), R("792", "Bantu"), R("793", "Mande"),
                R("794", "Fulani"), R("796", "Kru, Ibo, Yoruba"), R("799", "African"),
                R("806", "Other Algonquian languages"), R("862", "Apache"), R("864", "Navaho"), R("907", "Dakota"),
                R("924", "Keres"), R("933", "Cherokee"), R("964", "Zuni"), R("966", "American Indian"),
                R("985", "Other Indo-European languages"), R("986", "Other Asian languages"),
                R("988", "Other Pacific Island languages"), R("989", "Other specified African languages"),
                R("990", "Aleut-Eskimo languages"), R("992", "South/Central American Indian languages"),
                R("993", "Other Specified North American Indian languages"), R("994", "Other languages"),
                R("996", "Uncodable"))));
    colInfo.put("LANX", new Triple<>("language_other_than_english", ColumnType.LONG, longSpecial(//
        R("b", -1L), R("1", 1L), R("2", 0L))));
    colInfo.put("LNGI", new Triple<>("linguistic_isolation", ColumnType.STRING, new ReplaceFn(//
        R("b", "n/a"), R("1", "Not linguistically isolated"), R("2", "Linguistically isolated"))));
    colInfo.put("MAR",
        new Triple<>("marital_status", ColumnType.STRING,
            new ReplaceFn(//
                R("1", "Married"), R("2", "Widowed"), R("3", "Divorced"), R("4", "Separated"),
                R("5", "Never married or under 15 years old"))));
    colInfo.put("MHP", new Triple<>("mobile_home_cost_yearly", ColumnType.LONG, longSpecial(//
        R("bbbbb", -1L))));
    colInfo.put("MIG",
        new Triple<>("mobility_status", ColumnType.STRING,
            new ReplaceFn(//
                R("b", "n/a"), R("1", "Yes, same house (nonmovers)"), R("2", "No, outside US and Puerto Rico"),
                R("3", "No, different house in US or Puerto Rico"))));
    colInfo.put("MIGPUMA", new Triple<>("migration_puma", ColumnType.LONG, longSpecial(//
        R("bbbbb", -1L))));
    colInfo.put("MIGSP",
        new Triple<>("migration_recode", ColumnType.STRING,
            new ReplaceFn(//
                R("bbb", "N/A (person less than 1 year old/lived in same house 1 year ago)"), R("001", "Alabama/AL"),
                R("002", "Alaska/AK"), R("004", "Arizona/AZ"), R("005", "Arkansas/AR"), R("006", "California/CA"),
                R("008", "Colorado/CO"), R("009", "Connecticut/CT"), R("010", "Delaware/DE"),
                R("011", "District of Columbia/DC"), R("012", "Florida/FL"), R("013", "Georgia/GA"),
                R("015", "Hawaii/HI"), R("016", "Idaho/ID"), R("017", "Illinois/IL"), R("018", "Indiana/IN"),
                R("019", "Iowa/IA"), R("020", "Kansas/KS"), R("021", "Kentucky/KY"), R("022", "Louisiana/LA"),
                R("023", "Maine/ME"), R("024", "Maryland/MD"), R("025", "Massachusetts/MA"), R("026", "Michigan/MI"),
                R("027", "Minnesota/MN"), R("028", "Mississippi/MS"), R("029", "Missouri/MO"), R("030", "Montana/MT"),
                R("031", "Nebraska/NE"), R("032", "Nevada/NV"), R("033", "New Hampshire/NH"), R("034", "New Jersey/NJ"),
                R("035", "New Mexico/NM"), R("036", "New York/NY"), R("037", "North Carolina/NC"),
                R("038", "North Dakota/ND"), R("039", "Ohio/OH"), R("040", "Oklahoma/OK"), R("041", "Oregon/OR"),
                R("042", "Pennsylvania/PA"), R("044", "Rhode Island/RI"), R("045", "South Carolina/SC"),
                R("046", "South Dakota/SD"),
                R("047", "Tennessee/TN"), R("048", "Texas/TX"), R("049", "Utah/UT"), R("050", "Vermont/VT"), R("051",
                    "Virginia/VA"),
                R("053", "Washington/WA"), R("054", "West Virginia/WV"), R("055", "Wisconsin/WI"),
                R("056", "Wyoming/WY"), R("072", "Puerto Rico"), R("096", "US Island Areas, Not Specified"),
                R("109", "France"), R("110", "Germany"), R("111", "Northern Europe, Not Specified"),
                R("112", "Western Europe, Not Specified"), R("113", "Eastern Europe, Not Specified"), R("120", "Italy"),
                R("128", "Poland"), R("138", "United Kingdom, Excluding England"), R("139", "England"),
                R("163", "Russia"), R("164", "Ukraine"), R("169", "Other Europe, Not Specified"),
                R("207", "China, Hong Kong & Paracel Islands"), R("210", "India"), R("213", "Iraq"), R("214", "Israel"),
                R("215", "Japan"), R("217", "Korea"), R("233", "Philippines"), R("240", "Taiwan"), R("242", "Thailand"),
                R("247", "Vietnam"), R("251", "Eastern Asia, Not Specified"), R("252", "Western Asia, Not Specified"),
                R("253", "South Central Asia or Asia, Not Specified"), R("301", "Canada"), R("303", "Mexico"),
                R("312", "El Salvador"), R("313", "Guatemala"), R("314", "Honduras"),
                R("317", "Central America, Not Specified"), R("327", "Cuba"), R("329", "Domincan Republic"),
                R("333", "Jamaica"), R("344", "Caribbean and North America, Not Specified"), R("362", "Brazil"),
                R("364", "Colombia"), R("370", "Peru"), R("374", "South America, Not Specified"), R("462", "Africa"),
                R("463", "Eastern Africa, Not Specified"), R("464", "Northern Africa, Not Specified"),
                R("467", "Western Africa, Not Specified"), R("468", "Other Africa, Not Specified"),
                R("501", "Australia"),
                R("554", "Australian and New Zealand Subregions, Not Specified, Oceania and at Sea"))));
    colInfo.put("MIL",
        new Triple<>("military_service", ColumnType.STRING,
            new ReplaceFn(//
                R("b", "n/a"), R("1", "Yes, now on active duty"),
                R("2", "Yes, on active duty during the last 12 months, but not now"),
                R("3", "Yes, on active duty in the past, but not during the last 12 months"),
                R("4", "No, training for Reserves/National Guard only"), R("5", "No, never served in the military"))));
    colInfo.put("MLPA", new Triple<>("military_served_sept_2001_later", ColumnType.LONG, longSpecial(//
        R("b", -1L), R("0", 0L), R("1", 1L))));
    colInfo.put("MLPB", new Triple<>("military_served_aug_1990_to_aug_2001", ColumnType.LONG, longSpecial(//
        R("b", -1L), R("0", 0L), R("1", 1L))));
    colInfo.put("MLPC", new Triple<>("military_served_sept_1980_to_jul_1990", ColumnType.LONG, longSpecial(//
        R("b", -1L), R("0", 0L), R("1", 1L))));
    colInfo.put("MLPD", new Triple<>("military_served_may_1975_to_aug_1980", ColumnType.LONG, longSpecial(//
        R("b", -1L), R("0", 0L), R("1", 1L))));
    colInfo.put("MLPE", new Triple<>("military_served_vietnam_era", ColumnType.LONG, longSpecial(//
        R("b", -1L), R("0", 0L), R("1", 1L))));
    colInfo.put("MLPF", new Triple<>("military_served_mar_1961_to_jul_1964", ColumnType.LONG, longSpecial(//
        R("b", -1L), R("0", 0L), R("1", 1L))));
    colInfo.put("MLPG", new Triple<>("military_served_feb_1955_to_feb_1961", ColumnType.LONG, longSpecial(//
        R("b", -1L), R("0", 0L), R("1", 1L))));
    colInfo.put("MLPH", new Triple<>("military_served_korean_war", ColumnType.LONG, longSpecial(//
        R("b", -1L), R("0", 0L), R("1", 1L))));
    colInfo.put("MLPI", new Triple<>("military_served_jan_1947_to_jun_1950", ColumnType.LONG, longSpecial(//
        R("b", -1L), R("0", 0L), R("1", 1L))));
    colInfo.put("MLPJ", new Triple<>("military_served_ww2", ColumnType.LONG, longSpecial(//
        R("b", -1L), R("0", 0L), R("1", 1L))));
    colInfo.put("MLPK", new Triple<>("military_served_nov_1941_or_earlier", ColumnType.LONG, longSpecial(//
        R("b", -1L), R("0", 0L), R("1", 1L))));
    colInfo.put("MRGI", new Triple<>("first_mortgage_includes_insurance", ColumnType.LONG, longSpecial(//
        R("b", -1L), R("1", 1L), R("2", 0L))));
    colInfo.put("MRGP", new Triple<>("first_mortgage_payment", ColumnType.LONG, longSpecial(//
        R("bbbbb", -1L))));
    colInfo.put("MRGT", new Triple<>("first_mortgage_includes_taxes", ColumnType.LONG, longSpecial(//
        R("b", -1L), R("1", 1L), R("2", 0L))));
    colInfo.put("MRGX",
        new Triple<>("first_mortgage_status", ColumnType.STRING,
            new ReplaceFn(//
                R("b", "n/a"), R("1", "Mortgage deed of trust, or similar debt"), R("2", "Contract to purchase"),
                R("3", "None"))));
    colInfo.put("MSP",
        new Triple<>("married_spouse_status", ColumnType.STRING,
            new ReplaceFn(//
                R("b", "n/a"), R("1", "Now married, spouse present"), R("2", "Now married, spouse absent"),
                R("3", "Widowed"), R("4", "Divorced"), R("5", "Separated"), R("6", "Never married"))));
    colInfo.put("MV",
        new Triple<>("when_moved_in", ColumnType.STRING, new ReplaceFn(//
            R("b", "n/a"), R("1", "12 months or less"), R("2", "13 to 23 months"), R("3", "2 to 4 years"),
            R("4", "5 to 9 years"), R("5", "10 to 19 years"), R("6", "20 to 29 years"), R("7", "30 years or more"))));
    colInfo.put("NAICSP", new Triple<>("naics_industry", ColumnType.STRING,
        new ReplaceFn(//
            R("bbbbbbbb", "N/A (less than 16 years old/NILF who last worked more than 5 years ago or never worked)"),
            R("111", "AGR-CROP PRODUCTION"), R("112", "AGR-ANIMAL PRODUCTION"),
            R("1133", "AGR-LOGGING"), R("113M", "AGR-FORESTRY EXCEPT LOGGING"), R("114",
                "AGR-FISHING, HUNTING, AND TRAPPING"),
            R("115", "AGR-SUPPORT ACTIVITIES FOR AGRICULTURE AND FORESTRY"), R("211", "EXT-OIL AND GAS EXTRACTION"),
            R("2121", "EXT-COAL MINING"), R("2122", "EXT-METAL ORE MINING"),
            R("2123", "EXT-NONMETALLIC MINERAL MINING AND QUARRYING"), R("213", "EXT-SUPPORT ACTIVITIES FOR MINING"),
            R("21S", "EXT-NOT SPECIFIED TYPE OF MINING"),
            R("2211P", "UTL-ELECTRIC POWER GENERATION, TRANSMISSION AND DISTRIBUTION"),
            R("2212P", "UTL-NATURAL GAS DISTRIBUTION"), R("22132", "UTL-SEWAGE TREATMENT FACILITIES"),
            R("2213M", "UTL-WATER, STEAM, AIR CONDITIONING, AND IRRIGATION SYSTEMS"),
            R("221MP", "UTL-ELECTRIC AND GAS, AND OTHER COMBINATIONS"), R("22S", "UTL-NOT SPECIFIED UTILITIES"),
            R("23", "CON-CONSTRUCTION, INCL CLEANING DURING AND IMM AFTER"),
            R("3113", "MFG-SUGAR AND CONFECTIONERY PRODUCTS"),
            R("3114", "MFG-FRUIT AND VEGETABLE PRESERVING AND SPECIALTY FOODS"), R("3115", "MFG-DAIRY PRODUCTS"),
            R("3116", "MFG-ANIMAL SLAUGHTERING AND PROCESSING"), R("311811", "MFG-RETAIL BAKERIES"),
            R("3118Z", "MFG-BAKERIES, EXCEPT RETAIL"), R("311M1", "MFG-ANIMAL FOOD, GRAIN AND OILSEED MILLING"),
            R("311M2", "MFG-SEAFOOD AND OTHER MISCELLANEOUS FOODS, N.E.C."),
            R("311S", "MFG-NOT SPECIFIED FOOD INDUSTRIES"), R("3121", "MFG-BEVERAGE"), R("3122", "MFG-TOBACCO"),
            R("3131", "MFG-FIBER, YARN, AND THREAD MILLS"), R("3132Z", "MFG-FABRIC MILLS, EXCEPT KNITTING MILLS"),
            R("3133", "MFG-TEXTILE AND FABRIC FINISHING AND FABRIC COATING MILLS"),
            R("31411", "MFG-CARPET AND RUG MILLS"), R("314Z", "MFG-TEXTILE PRODUCT MILLS, EXCEPT CARPET AND RUG"),
            R("3152", "MFG-CUT AND SEW APPAREL"), R("3159", "MFG-APPAREL ACCESSORIES AND OTHER APPAREL"),
            R("3162", "MFG-FOOTWEAR"),
            R("316M", "MFG-LEATHER TANNING AND FINISHING AND OTHER ALLIED PRODUCTS MANUFACTURING"),
            R("31M", "MFG-KNITTING FABRIC MILLS, AND APPAREL KNITTING MILLS"),
            R("3211", "MFG-SAWMILLS AND WOOD PRESERVATION"),
            R("3212", "MFG-VENEER, PLYWOOD, AND ENGINEERED WOOD PRODUCTS"),
            R("32199M", "MFG-PREFABRICATED WOOD BUILDINGS AND MOBILE HOMES"),
            R("3219ZM", "MFG-MISCELLANEOUS WOOD PRODUCTS"), R("3221", "MFG-PULP, PAPER, AND PAPERBOARD MILLS"),
            R("32221", "MFG-PAPERBOARD CONTAINERS AND BOXES"), R("3222M", "MFG-MISCELLANEOUS PAPER AND PULP PRODUCTS"),
            R("3231", "MFG-PRINTING AND RELATED SUPPORT ACTIVITIES"), R("32411", "MFG-PETROLEUM REFINING"),
            R("3241M", "MFG-MISCELLANEOUS PETROLEUM AND COAL PRODUCTS"),
            R("3252", "MFG-RESIN, SYNTHETIC RUBBER, AND FIBERS AND FILAMENTS"), R("3253", "MFG-AGRICULTURAL CHEMICALS"),
            R("3254", "MFG-PHARMACEUTICALS AND MEDICINES"), R("3255", "MFG-PAINT, COATING, AND ADHESIVES"),
            R("3256", "MFG-SOAP, CLEANING COMPOUND, AND COSMETICS"),
            R("325M", "MFG-INDUSTRIAL AND MISCELLANEOUS CHEMICALS"), R("3261", "MFG-PLASTICS PRODUCTS"),
            R("32621", "MFG-TIRES"), R("3262M", "MFG-RUBBER PRODUCTS, EXCEPT TIRES"),
            R("32711", "MFG-POTTERY, CERAMICS, AND PLUMBING FIXTURE MANUFACTURING"),
            R("32712", "MFG-STRUCTURAL CLAY PRODUCTS"), R("3272", "MFG-GLASS AND GLASS PRODUCTS"),
            R("3279", "MFG-MISCELLANEOUS NONMETALLIC MINERAL PRODUCTS"),
            R("327M", "MFG-CEMENT, CONCRETE, LIME, AND GYPSUM PRODUCTS"),
            R("3313", "MFG-ALUMINUM PRODUCTION AND PROCESSING"),
            R("3314", "MFG-NONFERROUS METAL, EXCEPT ALUMINUM, PRODUCTION AND PROCESSING"), R("3315", "MFG-FOUNDRIES"),
            R("331M", "MFG-IRON AND STEEL MILLS AND STEEL PRODUCTS"), R("3321", "MFG-METAL FORGINGS AND STAMPINGS"),
            R("3322", "MFG-CUTLERY AND HAND TOOLS"),
            R("3327", "MFG-MACHINE SHOPS; TURNED PRODUCTS; SCREWS, NUTS AND BOLTS"),
            R("3328", "MFG-COATING, ENGRAVING, HEAT TREATING AND ALLIED ACTIVITIES"), R("33299M", "MFG-ORDNANCE"),
            R("332M", "MFG-STRUCTURAL METALS, AND BOILER, TANK, AND SHIPPING CONTAINERS"),
            R("332MZ", "MFG-MISCELLANEOUS FABRICATED METAL PRODUCTS"), R("33311", "MFG-AGRICULTURAL IMPLEMENTS"),
            R("3331M", "MFG-CONSTRUCTION, AND MINING AND OIL AND GAS FIELD MACHINERY"),
            R("3333", "MFG-COMMERCIAL AND SERVICE INDUSTRY MACHINERY"), R("3335", "MFG-METALWORKING MACHINERY"),
            R("3336", "MFG-ENGINES, TURBINES, AND POWER TRANSMISSION EQUIPMENT"), R("333M", "MFG-MACHINERY, N.E.C."),
            R("333S", "MFG-NOT SPECIFIED MACHINERY"), R("3341", "MFG-COMPUTER AND PERIPHERAL EQUIPMENT"),
            R("3345", "MFG-NAVIGATIONAL, MEASURING, ELECTROMEDICAL, AND CONTROL INSTRUMENTS"),
            R("334M1", "MFG-COMMUNICATIONS, AND AUDIO AND VIDEO EQUIPMENT"),
            R("334M2", "MFG-ELECTRONIC COMPONENTS AND PRODUCTS, N.E.C."), R("3352", "MFG-HOUSEHOLD APPLIANCES"),
            R("335M",
                "MFG-ELECTRIC LIGHTING AND ELECTRICAL EQUIPMENT MANUFACTURING, AND OTHER ELECTRICAL COMPONENT MANUFACTURING, N.E.C."),
        R("33641M1", "MFG-AIRCRAFT AND PARTS"), R("33641M2", "MFG-AEROSPACE PRODUCTS AND PARTS"),
        R("3365", "MFG-RAILROAD ROLLING STOCK"), R("3366", "MFG-SHIP AND BOAT BUILDING"),
        R("3369", "MFG-OTHER TRANSPORTATION EQUIPMENT"), R("336M", "MFG-MOTOR VEHICLES AND MOTOR VEHICLE EQUIPMENT"),
        R("337", "MFG-FURNITURE AND RELATED PRODUCTS"), R("3391", "MFG-MEDICAL EQUIPMENT AND SUPPLIES"),
        R("3399M", "MFG-SPORTING AND ATHLETIC GOODS, AND DOLL, TOY, AND GAME MANUFACTURING"),
        R("3399ZM", "MFG-MISCELLANEOUS MANUFACTURING, N.E.C."), R("33MS", "MFG-NOT SPECIFIED METAL INDUSTRIES"),
        R("3MS", "MFG-NOT SPECIFIED INDUSTRIES"),
        R("4231", "WHL-MOTOR VEHICLES, PARTS AND SUPPLIES MERCHANT WHOLESALERS"),
        R("4232", "WHL-FURNITURE AND HOME FURNISHING MERCHANT WHOLESALERS"),
        R("4233", "WHL-LUMBER AND OTHER CONSTRUCTION MATERIALS MERCHANT WHOLESALERS"),
        R("4234", "WHL-PROFESSIONAL AND COMMERCIAL EQUIPMENT AND SUPPLIES MERCHANT WHOLESALERS"),
        R("4235", "WHL-METALS AND MINERALS, EXCEPT PETROLEUM, MERCHANT WHOLESALERS"),
        R("4236", "WHL-ELECTRICAL AND ELECTRONIC GOODS MERCHANT WHOLESALERS"),
        R("4237", "WHL-HARDWARE, PLUMBING AND HEATING EQUIPMENT, AND SUPPLIES MERCHANT WHOLESALERS"),
        R("4238", "WHL-MACHINERY, EQUIPMENT, AND SUPPLIES MERCHANT WHOLESALERS"),
        R("42393", "WHL-RECYCLABLE MATERIAL MERCHANT WHOLESALERS"),
        R("4239Z", "WHL-MISCELLANEOUS DURABLE GOODS MERCHANT WHOLESALERS"),
        R("4241", "WHL-PAPER AND PAPER PRODUCTS MERCHANT WHOLESALERS"),
        R("4243", "WHL-APPAREL, FABRICS, AND NOTIONS MERCHANT WHOLESALERS"),
        R("4244", "WHL-GROCERIES AND RELATED PRODUCTS MERCHANT WHOLESALERS"),
        R("4245", "WHL-FARM PRODUCT RAW MATERIALS MERCHANT WHOLESALERS"),
        R("4247", "WHL-PETROLEUM AND PETROLEUM PRODUCTS MERCHANT WHOLESALERS"),
        R("4248", "WHL-ALCOHOLIC BEVERAGES MERCHANT WHOLESALERS"), R("42491", "WHL-FARM SUPPLIES MERCHANT WHOLESALERS"),
        R("4249Z", "WHL-MISCELLANEOUS NONDURABLE GOODS MERCHANT WHOLESALERS"),
        R("424M", "WHL-DRUGS, SUNDRIES, AND CHEMICAL AND ALLIED PRODUCTS MERCHANT WHOLESALERS"),
        R("4251", "WHL-ELECTRONIC MARKETS AND AGENTS AND BROKERS"), R("42S", "WHL-NOT SPECIFIED TRADE"),
        R("4411", "RET-AUTOMOBILE DEALERS"), R("4412", "RET-OTHER MOTOR VEHICLE DEALERS"),
        R("4413", "RET-AUTO PARTS, ACCESSORIES, AND TIRE STORES"),
        R("442", "RET-FURNITURE AND HOME FURNISHINGS STORES"), R("443111", "RET-HOUSEHOLD APPLIANCE STORES"),
        R("4431M", "RET-RADIO, TV, AND COMPUTER STORES"), R("44413", "RET-HARDWARE STORES"),
        R("4441Z", "RET-BUILDING MATERIAL AND SUPPLIES DEALERS"),
        R("4442", "RET-LAWN AND GARDEN EQUIPMENT AND SUPPLIES STORES"), R("4451", "RET-GROCERY STORES"),
        R("4452", "RET-SPECIALTY FOOD STORES"), R("4453", "RET-BEER, WINE, AND LIQUOR STORES"),
        R("44611", "RET-PHARMACIES AND DRUG STORES"), R("446Z", "RET-HEALTH AND PERSONAL CARE, EXCEPT DRUG, STORES"),
        R("447", "RET-GASOLINE STATIONS"), R("44821", "RET-SHOE STORES"),
        R("4483", "RET-JEWELRY, LUGGAGE, AND LEATHER GOODS STORES"), R("4481", "RET-CLOTHING STORES"),
        R("45113", "RET-SEWING, NEEDLEWORK AND PIECE GOODS STORES"), R("45121", "RET-BOOK STORES AND NEWS DEALERS"),
        R("451M", "RET-MUSIC STORES"), R("45211", "RET-DEPARTMENT AND DISCOUNT STORES"),
        R("4529", "RET-MISCELLANEOUS GENERAL MERCHANDISE STORES"), R("4531", "RET-FLORISTS"),
        R("45321", "RET-OFFICE SUPPLIES AND STATIONERY STORES"), R("45322", "RET-GIFT, NOVELTY, AND SOUVENIR SHOPS"),
        R("4533", "RET-USED MERCHANDISE STORES"), R("4539", "RET-MISCELLANEOUS RETAIL STORES"),
        R("454111", "RET-ELECTRONIC SHOPPING"), R("454112", "RET-ELECTRONIC AUCTIONS"),
        R("454113", "RET-MAIL-ORDER HOUSES"), R("4542", "RET-VENDING MACHINE OPERATORS"),
        R("45431", "RET-FUEL DEALERS"), R("45439", "RET-OTHER DIRECT SELLING ESTABLISHMENTS"),
        R("4M", "RET-SPORTING GOODS, CAMERA, AND HOBBY AND TOY STORES"), R("4MS", "RET-NOT SPECIFIED TRADE"),
        R("481", "TRN-AIR TRANSPORTATION"), R("482", "TRN-RAIL TRANSPORTATION"), R("483", "TRN-WATER TRANSPORTATION"),
        R("484", "TRN-TRUCK TRANSPORTATION"), R("4853", "TRN-TAXI AND LIMOUSINE SERVICE"),
        R("485M", "TRN-BUS SERVICE AND URBAN TRANSIT"), R("486", "TRN-PIPELINE TRANSPORTATION"),
        R("487", "TRN-SCENIC AND SIGHTSEEING TRANSPORTATION"), R("488", "TRN-SERVICES INCIDENTAL TO TRANSPORTATION"),
        R("491", "TRN-POSTAL SERVICE"), R("492", "TRN-COURIERS AND MESSENGERS"),
        R("493", "TRN-WAREHOUSING AND STORAGE"), R("51111", "INF-NEWSPAPER PUBLISHERS"),
        R("5111Z", "INF-PERIODICAL, BOOK, AND DIRECTORY PUBLISHERS"), R("5112", "INF-SOFTWARE PUBLISHERS"),
        R("5121", "INF-MOTION PICTURE AND VIDEO INDUSTRIES"), R("5122", "INF-SOUND RECORDING INDUSTRIES"),
        R("5171", "INF-WIRED TELECOMMUNICATIONS CARRIERS"), R("517Z", "INF-OTHER TELECOMMUNICATION SERVICES"),
        R("5182", "INF-DATA PROCESSING, HOSTING, AND RELATED SERVICES"), R("51912", "INF-LIBRARIES AND ARCHIVES"),
        R("5191ZM",
            "INF-OTHER INFORMATION SERVICES, EXC. LIBRARIES AND ARCHIVES, AND INTERNET PUBLISHING AND BROADCASTING AND WEB SEARCH PORTALS"),
        R("515", "INF-BROADCASTING, INCLUDING INTERNET PUBLISHING AND WEB SEARCH PORTALS"),
        R("5221M", "FIN-SAVINGS INSTITUTIONS, INCLUDING CREDIT UNIONS"),
        R("522M", "FIN-NON-DEPOSITORY CREDIT AND RELATED ACTIVITIES"),
        R("524", "FIN-INSURANCE CARRIERS AND RELATED ACTIVITIES"), R("52M1", "FIN-BANKING AND RELATED ACTIVITIES"),
        R("52M2", "FIN-SECURITIES, COMMODITIES, FUNDS, TRUSTS, AND OTHER FINANCIAL INVESTMENTS"),
        R("531", "FIN-REAL ESTATE"), R("5321", "FIN-AUTOMOTIVE EQUIPMENT RENTAL AND LEASING"),
        R("53223", "FIN-VIDEO TAPE AND DISK RENTAL"), R("532M", "FIN-OTHER CONSUMER GOODS RENTAL"),
        R("53M", "FIN-COMMERCIAL, INDUSTRIAL, AND OTHER INTANGIBLE ASSETS RENTAL AND LEASING"),
        R("5411", "PRF-LEGAL SERVICES"), R("5412", "PRF-ACCOUNTING, TAX PREPARATION, BOOKKEEPING AND PAYROLL SERVICES"),
        R("5413", "PRF-ARCHITECTURAL, ENGINEERING, AND RELATED SERVICES"), R("5414", "PRF-SPECIALIZED DESIGN SERVICES"),
        R("5415", "PRF-COMPUTER SYSTEMS DESIGN AND RELATED SERVICES"),
        R("5416", "PRF-MANAGEMENT, SCIENTIFIC AND TECHNICAL CONSULTING SERVICES"),
        R("5417", "PRF-SCIENTIFIC RESEARCH AND DEVELOPMENT SERVICES"),
        R("5418", "PRF-ADVERTISING AND RELATED SERVICES"), R("54194", "PRF-VETERINARY SERVICES"),
        R("5419Z", "PRF-OTHER PROFESSIONAL, SCIENTIFIC AND TECHNICAL SERVICES"),
        R("55", "PRF-MANAGEMENT OF COMPANIES AND ENTERPRISES"), R("5613", "PRF-EMPLOYMENT SERVICES"),
        R("5614", "PRF-BUSINESS SUPPORT SERVICES"), R("5615", "PRF-TRAVEL ARRANGEMENTS AND RESERVATION SERVICES"),
        R("5616", "PRF-INVESTIGATION AND SECURITY SERVICES"), R("56173", "PRF-LANDSCAPING SERVICES"),
        R("5617Z", "PRF-SERVICES TO BUILDINGS AND DWELLINGS, EX CONSTR CLN"),
        R("561M", "PRF-OTHER ADMINISTRATIVE, AND OTHER SUPPORT SERVICES"),
        R("562", "PRF-WASTE MANAGEMENT AND REMEDIATION SERVICES"), R("6111", "EDU-ELEMENTARY AND SECONDARY SCHOOLS"),
        R("611M1", "EDU-COLLEGES AND UNIVERSITIES, INCLUDING JUNIOR COLLEGES"),
        R("611M2", "EDU-BUSINESS, TECHNICAL, AND TRADE SCHOOLS AND TRAINING"),
        R("611M3", "EDU-OTHER SCHOOLS AND INSTRUCTION, AND EDUCATIONAL SUPPORT SERVICES"),
        R("6211", "MED-OFFICES OF PHYSICIANS"), R("6212", "MED-OFFICES OF DENTISTS"),
        R("62131", "MED-OFFICE OF CHIROPRACTORS"), R("62132", "MED-OFFICES OF OPTOMETRISTS"),
        R("6213ZM", "MED-OFFICES OF OTHER HEALTH PRACTITIONERS"), R("6214", "MED-OUTPATIENT CARE CENTERS"),
        R("6216", "MED-HOME HEALTH CARE SERVICES"), R("621M", "MED-OTHER HEALTH CARE SERVICES"),
        R("622", "MED-HOSPITALS"), R("6231", "MED-NURSING CARE FACILITIES"),
        R("623M", "MED-RESIDENTIAL CARE FACILITIES, WITHOUT NURSING"), R("6241", "SCA-INDIVIDUAL AND FAMILY SERVICES"),
        R("6242", "SCA-COMMUNITY FOOD AND HOUSING, AND EMERGENCY SERVICES"),
        R("6243", "SCA-VOCATIONAL REHABILITATION SERVICES"), R("6244", "SCA-CHILD DAY CARE SERVICES"),
        R("711", "ENT-INDEPENDENT ARTISTS, PERFORMING ARTS, SPECTATOR SPORTS AND RELATED INDUSTRIES"),
        R("712", "ENT-MUSEUMS, ART GALLERIES, HISTORICAL SITES, AND SIMILAR INSTITUTIONS"),
        R("71395", "ENT-BOWLING CENTERS"), R("713Z", "ENT-OTHER AMUSEMENT, GAMBLING, AND RECREATION INDUSTRIES"),
        R("7211", "ENT-TRAVELER ACCOMMODATION"),
        R("721M", "ENT-RECREATIONAL VEHICLE PARKS AND CAMPS, AND ROOMING AND BOARDING HOUSES"),
        R("7224", "ENT-DRINKING PLACES, ALCOHOLIC BEVERAGES"), R("722Z", "ENT-RESTAURANTS AND OTHER FOOD SERVICES"),
        R("811192", "SRV-CAR WASHES"), R("8111Z", "SRV-AUTOMOTIVE REPAIR AND MAINTENANCE"),
        R("8112", "SRV-ELECTRONIC AND PRECISION EQUIPMENT REPAIR AND MAINTENANCE"),
        R("8113", "SRV-COMMERCIAL AND INDUSTRIAL MACHINERY AND EQUIPMENT REPAIR AND MAINTENANCE"),
        R("8114", "SRV-PERSONAL AND HOUSEHOLD GOODS REPAIR AND MAINTENANCE"), R("812111", "SRV-BARBER SHOPS"),
        R("812112", "SRV-BEAUTY SALONS"), R("8121M", "SRV-NAIL SALONS AND OTHER PERSONAL CARE SERVICES"),
        R("8122", "SRV-FUNERAL HOMES, CEMETERIES AND CREMATORIES"), R("8123", "SRV-DRYCLEANING AND LAUNDRY SERVICES"),
        R("8129", "SRV-OTHER PERSONAL SERVICES"), R("8131", "SRV-RELIGIOUS ORGANIZATIONS"),
        R("81393", "SRV-LABOR UNIONS"), R("8139Z", "SRV-BUSINESS, PROFESSIONAL, POLITICAL AND SIMILAR ORGANIZATIONS"),
        R("813M", "SRV-CIVIC, SOCIAL, ADVOCACY ORGANIZATIONS, AND GRANTMAKING AND GIVING SERVICES"),
        R("814", "SRV-PRIVATE HOUSEHOLDS"), R("92113", "ADM-PUBLIC FINANCE ACTIVITIES"),
        R("92119", "ADM-OTHER GENERAL GOVERNMENT AND SUPPORT"),
        R("9211MP", "ADM-EXECUTIVE OFFICES AND LEGISLATIVE BODIES"),
        R("923", "ADM-ADMINISTRATION OF HUMAN RESOURCE PROGRAMS"), R("928110P1", "MIL-U.S. ARMY"),
        R("928110P2", "MIL-U.S. AIR FORCE"), R("928110P3", "MIL-U.S. NAVY"), R("928110P4", "MIL-U.S. MARINES"),
        R("928110P5", "MIL-U.S. COAST GUARD"), R("928110P6", "MIL-U.S. ARMED FORCES, BRANCH NOT SPECIFIED"),
        R("928110P7", "MIL-MILITARY RESERVES OR NATIONAL GUARD"),
        R("928P", "ADM-NATIONAL SECURITY AND INTERNATIONAL AFFAIRS"),
        R("92M1", "ADM-ADMINISTRATION OF ENVIRONMENTAL QUALITY AND HOUSING PROGRAMS"),
        R("92M2", "ADM-ADMINISTRATION OF ECONOMIC PROGRAMS AND SPACE RESEARCH"),
        R("92MP", "ADM-JUSTICE, PUBLIC ORDER, AND SAFETY ACTIVITIES"),
        R("9920", "UNEMPLOYED AND LAST WORKED 5 YEARS AGO OR EARLIER OR NEVER WORKED"))));
    colInfo.put("NATIVITY", new Triple<>("nativity", ColumnType.STRING, new ReplaceFn(//
        R("1", "native"), R("2", "foreign born"))));
    colInfo.put("NOC", new Triple<>("number_of_own_children_in_household", ColumnType.LONG, longSpecial(//
        R("bb", -1L))));
    colInfo.put("NP", new Triple<>("number_of_persons", ColumnType.LONG, longFn()));
    colInfo.put("NPF", new Triple<>("number_of_persons_in_family", ColumnType.LONG, longSpecial(//
        R("bb", -1L))));
    colInfo.put("NPP", new Triple<>("grandparent_household", ColumnType.LONG, longSpecial(//
        R("b", -1L), R("0", 0L), R("1", 1L))));
    colInfo.put("NR", new Triple<>("nonrelative_in_household", ColumnType.LONG, longSpecial(//
        R("b", -1L), R("0", 0L), R("1", 1L))));
    colInfo.put("NRC", new Triple<>("number_of_related_children_in_household", ColumnType.LONG, longSpecial(//
        R("bb", -1L))));
    colInfo.put("NWAB", new Triple<>("temporary_absence_from_work_unedited", ColumnType.STRING, new ReplaceFn(//
        R("b", "n/a"), R("1", "yes"), R("2", "no"), R("3", "did not report"))));
    colInfo.put("NWAV",
        new Triple<>("available_for_work_unedited", ColumnType.STRING,
            new ReplaceFn(//
                R("b", "n/a"), R("1", "yes"), R("2", "no, temporary ill"), R("3", "no, other reasons"),
                R("4", "no, unspecified"), R("5", "did not report"))));
    colInfo.put("NWLA", new Triple<>("layoff_from_work_unedited", ColumnType.STRING, new ReplaceFn(//
        R("b", "n/a"), R("1", "yes"), R("2", "no"), R("3", "did not report"))));
    colInfo.put("NWLK", new Triple<>("looking_for_work_unedited", ColumnType.STRING, new ReplaceFn(//
        R("b", "n/a"), R("1", "yes"), R("2", "no"), R("3", "did not report"))));
    colInfo.put("NWRE", new Triple<>("informed_of_recall_work_unedited", ColumnType.STRING, new ReplaceFn(//
        R("b", "n/a"), R("1", "yes"), R("2", "no"), R("3", "did not report"))));
    colInfo.put("OC", new Triple<>("own_child", ColumnType.LONG, bool()));
    colInfo.put("OCCP",
        new Triple<>("occupation_recode", ColumnType.STRING, new ReplaceFn(//
            R("bbbb", "N/A (less than 16 years old/NILF who last worked more than 5 years ago or never worked)"),
            R("0010", "MGR-CHIEF EXECUTIVES AND LEGISLATORS"), R("0020", "MGR-GENERAL AND OPERATIONS MANAGERS"),
            R("0040", "MGR-ADVERTISING AND PROMOTIONS MANAGERS"), R("0050", "MGR-MARKETING AND SALES MANAGERS"),
            R("0060", "MGR-PUBLIC RELATIONS MANAGERS"), R("0100", "MGR-ADMINISTRATIVE SERVICES MANAGERS"),
            R("0110", "MGR-COMPUTER AND INFORMATION SYSTEMS MANAGERS"), R("0120", "MGR-FINANCIAL MANAGERS"),
            R("0130", "MGR-HUMAN RESOURCES MANAGERS"), R("0140", "MGR-INDUSTRIAL PRODUCTION MANAGERS"),
            R("0150", "MGR-PURCHASING MANAGERS"), R("0160", "MGR-TRANSPORTATION, STORAGE, AND DISTRIBUTION MANAGERS"),
            R("0200", "MGR-FARM, RANCH, AND OTHER AGRICULTURAL MANAGERS"), R("0210", "MGR-FARMERS AND RANCHERS"),
            R("0220", "MGR-CONSTRUCTION MANAGERS"), R("0230", "MGR-EDUCATION ADMINISTRATORS"),
            R("0300", "MGR-ENGINEERING MANAGERS"), R("0310", "MGR-FOOD SERVICE MANAGERS"),
            R("0320", "MGR-FUNERAL DIRECTORS"), R("0330", "MGR-GAMING MANAGERS"), R("0340", "MGR-LODGING MANAGERS"),
            R("0350", "MGR-MEDICAL AND HEALTH SERVICES MANAGERS"), R("0360", "MGR-NATURAL SCIENCES MANAGERS"),
            R("0410", "MGR-PROPERTY, REAL ESTATE, AND COMMUNITY ASSOCIATION MANAGERS"),
            R("0420", "MGR-SOCIAL AND COMMUNITY SERVICE MANAGERS"),
            R("0430", "MGR-MISCELLANEOUS MANAGERS, INCLUDING POSTMASTERS AND MAIL SUPERINTENDENTS "),
            R("0500", "BUS-AGENTS AND BUSINESS MANAGERS OF ARTISTS, PERFORMERS, AND ATHLETES"),
            R("0510", "BUS-PURCHASING AGENTS AND BUYERS, FARM PRODUCTS"),
            R("0520", "BUS-WHOLESALE AND RETAIL BUYERS, EXCEPT FARM PRODUCTS"),
            R("0530", "BUS-PURCHASING AGENTS, EXCEPT WHOLESALE, RETAIL, AND FARM PRODUCTS"),
            R("0540", "BUS-CLAIMS ADJUSTERS, APPRAISERS, EXAMINERS, AND INVESTIGATORS"),
            R("0560",
                "BUS-COMPLIANCE OFFICERS, EXCEPT AGRICULTURE, CONSTRUCTION, HEALTH AND SAFETY, AND TRANSPORTATION"),
        R("0600", "BUS-COST ESTIMATORS"), R("0620", "BUS-HUMAN RESOURCES, TRAINING, AND LABOR RELATIONS SPECIALISTS"),
        R("0700", "BUS-LOGISTICIANS"), R("0710", "BUS-MANAGEMENT ANALYSTS"),
        R("0720", "BUS-MEETING AND CONVENTION PLANNERS"), R("0730", "BUS-OTHER BUSINESS OPERATIONS SPECIALISTS"),
        R("0800", "FIN-ACCOUNTANTS AND AUDITORS"), R("0810", "FIN-APPRAISERS AND ASSESSORS OF REAL ESTATE"),
        R("0820", "FIN-BUDGET ANALYSTS"), R("0830", "FIN-CREDIT ANALYSTS"), R("0840", "FIN-FINANCIAL ANALYSTS"),
        R("0850", "FIN-PERSONAL FINANCIAL ADVISORS"), R("0860", "FIN-INSURANCE UNDERWRITERS"),
        R("0900", "FIN-FINANCIAL EXAMINERS"), R("0910", "FIN-LOAN COUNSELORS AND OFFICERS"),
        R("0930", "FIN-TAX EXAMINERS, COLLECTORS, AND REVENUE AGENTS"), R("0940", "FIN-TAX PREPARERS"),
        R("0950", "FIN-FINANCIAL SPECIALISTS, ALL OTHER"), R("1000", "CMM-COMPUTER SCIENTISTS AND SYSTEMS ANALYSTS"),
        R("1010", "CMM-COMPUTER PROGRAMMERS"), R("1020", "CMM-COMPUTER SOFTWARE ENGINEERS"),
        R("1040", "CMM-COMPUTER SUPPORT SPECIALISTS"), R("1060", "CMM-DATABASE ADMINISTRATORS"),
        R("1100", "CMM-NETWORK AND COMPUTER SYSTEMS ADMINISTRATORS"),
        R("1110", "CMM-NETWORK SYSTEMS AND DATA COMMUNICATIONS ANALYSTS"), R("1200", "CMM-ACTUARIES"),
        R("1220", "CMM-OPERATIONS RESEARCH ANALYSTS"),
        R("1240", "CMM-MISCELLANEOUS MATHEMATICAL SCIENCE OCCUPATIONS, INCLUDING MATHEMATICIANS AND STATISTICIANS"),
        R("1300", "ENG-ARCHITECTS, EXCEPT NAVAL"), R("1310", "ENG-SURVEYORS, CARTOGRAPHERS, AND PHOTOGRAMMETRISTS"),
        R("1320", "ENG-AEROSPACE ENGINEERS"), R("1340", "ENG-BIOMEDICAL AND AGRICULTURAL ENGINEERS"),
        R("1350", "ENG-CHEMICAL ENGINEERS"), R("1360", "ENG-CIVIL ENGINEERS"),
        R("1400", "ENG-COMPUTER HARDWARE ENGINEERS"), R("1410", "ENG-ELECTRICAL AND ELECTRONICS ENGINEERS"),
        R("1420", "ENG-ENVIRONMENTAL ENGINEERS"), R("1430", "ENG-INDUSTRIAL ENGINEERS, INCLUDING HEALTH AND SAFETY"),
        R("1440", "ENG-MARINE ENGINEERS AND NAVAL ARCHITECTS"), R("1450", "ENG-MATERIALS ENGINEERS"),
        R("1460", "ENG-MECHANICAL ENGINEERS"),
        R("1520", "ENG-PETROLEUM, MINING AND GEOLOGICAL ENGINEERS, INCLUDING MINING SAFETY ENGINEERS"),
        R("1530", "ENG-MISCELLANEOUS ENGINEERS, INCLUDING NUCLEAR ENGINEERS"), R("1540", "ENG-DRAFTERS"),
        R("1550", "ENG-ENGINEERING TECHNICIANS, EXCEPT DRAFTERS"), R("1560", "ENG-SURVEYING AND MAPPING TECHNICIANS"),
        R("1600", "SCI-AGRICULTURAL AND FOOD SCIENTISTS"), R("1610", "SCI-BIOLOGICAL SCIENTISTS"),
        R("1640", "SCI-CONSERVATION SCIENTISTS AND FORESTERS"), R("1650", "SCI-MEDICAL SCIENTISTS"),
        R("1700", "SCI-ASTRONOMERS AND PHYSICISTS"), R("1710", "SCI-ATMOSPHERIC AND SPACE SCIENTISTS"),
        R("1720", "SCI-CHEMISTS AND MATERIALS SCIENTISTS"), R("1740", "SCI-ENVIRONMENTAL SCIENTISTS AND GEOSCIENTISTS"),
        R("1760", "SCI-PHYSICAL SCIENTISTS, ALL OTHER"), R("1800", "SCI-ECONOMISTS"),
        R("1810", "SCI-MARKET AND SURVEY RESEARCHERS"), R("1820", "SCI-PSYCHOLOGISTS"),
        R("1840", "SCI-URBAN AND REGIONAL PLANNERS"),
        R("1860", "SCI-MISCELLANEOUS SOCIAL SCIENTISTS, INCLUDING SOCIOLOGISTS"),
        R("1900", "SCI-AGRICULTURAL AND FOOD SCIENCE TECHNICIANS"), R("1910", "SCI-BIOLOGICAL TECHNICIANS"),
        R("1920", "SCI-CHEMICAL TECHNICIANS"), R("1930", "SCI-GEOLOGICAL AND PETROLEUM TECHNICIANS"),
        R("1960",
            "SCI-MISCELLANEOUS LIFE, PHYSICAL, AND SOCIAL SCIENCE TECHNICIANS, INCLUDING SOCIAL SCIENCE RESEARCH ASSISTANTS AND NUCLEAR TECHNICIANS"),
        R("2000", "CMS-COUNSELORS"), R("2010", "CMS-SOCIAL WORKERS"),
        R("2020", "CMS-MISCELLANEOUS COMMUNITY AND SOCIAL SERVICE SPECIALISTS"), R("2040", "CMS-CLERGY"),
        R("2050", "CMS-DIRECTORS, RELIGIOUS ACTIVITIES AND EDUCATION"), R("2060", "CMS-RELIGIOUS WORKERS, ALL OTHER"),
        R("2100", "LGL-LAWYERS, AND JUDGES, MAGISTRATES, AND OTHER JUDICIAL WORKERS"),
        R("2140", "LGL-PARALEGALS AND LEGAL ASSISTANTS"), R("2150", "LGL-MISCELLANEOUS LEGAL SUPPORT WORKERS"),
        R("2200", "EDU-POSTSECONDARY TEACHERS"), R("2300", "EDU-PRESCHOOL AND KINDERGARTEN TEACHERS"),
        R("2310", "EDU-ELEMENTARY AND MIDDLE SCHOOL TEACHERS"), R("2320", "EDU-SECONDARY SCHOOL TEACHERS"),
        R("2330", "EDU-SPECIAL EDUCATION TEACHERS"), R("2340", "EDU-OTHER TEACHERS AND INSTRUCTORS"),
        R("2400", "EDU-ARCHIVISTS, CURATORS, AND MUSEUM TECHNICIANS"), R("2430", "EDU-LIBRARIANS"),
        R("2440", "EDU-LIBRARY TECHNICIANS"), R("2540", "EDU-TEACHER ASSISTANTS"),
        R("2550", "EDU-OTHER EDUCATION, TRAINING, AND LIBRARY WORKERS"), R("2600", "ENT-ARTISTS AND RELATED WORKERS"),
        R("2630", "ENT-DESIGNERS"), R("2700", "ENT-ACTORS"), R("2710", "ENT-PRODUCERS AND DIRECTORS"),
        R("2720", "ENT-ATHLETES, COACHES, UMPIRES, AND RELATED WORKERS"), R("2740", "ENT-DANCERS AND CHOREOGRAPHERS"),
        R("2750", "ENT-MUSICIANS, SINGERS, AND RELATED WORKERS"),
        R("2760", "ENT-ENTERTAINERS AND PERFORMERS, SPORTS AND RELATED WORKERS, ALL OTHER"),
        R("2800", "ENT-ANNOUNCERS"), R("2810", "ENT-NEWS ANALYSTS, REPORTERS AND CORRESPONDENTS"),
        R("2820", "ENT-PUBLIC RELATIONS SPECIALISTS"), R("2830", "ENT-EDITORS"), R("2840", "ENT-TECHNICAL WRITERS"),
        R("2850", "ENT-WRITERS AND AUTHORS"), R("2860", "ENT-MISCELLANEOUS MEDIA AND COMMUNICATION WORKERS"),
        R("2900",
            "ENT-BROADCAST AND SOUND ENGINEERING TECHNICIANS AND RADIO OPERATORS, AND MEDIA AND COMMUNICATION EQUIPMENT WORKERS, ALL OTHER"),
        R("2910", "ENT-PHOTOGRAPHERS"),
        R("2920", "ENT-TELEVISION, VIDEO, AND MOTION PICTURE CAMERA OPERATORS AND EDITORS"),
        R("3000", "MED-CHIROPRACTORS"), R("3010", "MED-DENTISTS"), R("3030", "MED-DIETITIANS AND NUTRITIONISTS"),
        R("3040", "MED-OPTOMETRISTS"), R("3050", "MED-PHARMACISTS"), R("3060", "MED-PHYSICIANS AND SURGEONS"),
        R("3110", "MED-PHYSICIAN ASSISTANTS"), R("3120", "MED-PODIATRISTS"), R("3130", "MED-REGISTERED NURSES"),
        R("3140", "MED-AUDIOLOGISTS"), R("3150", "MED-OCCUPATIONAL THERAPISTS"), R("3160", "MED-PHYSICAL THERAPISTS"),
        R("3200", "MED-RADIATION THERAPISTS"), R("3210", "MED-RECREATIONAL THERAPISTS"),
        R("3220", "MED-RESPIRATORY THERAPISTS"), R("3230", "MED-SPEECH-LANGUAGE PATHOLOGISTS"),
        R("3240", "MED-THERAPISTS, ALL OTHER"), R("3250", "MED-VETERINARIANS"),
        R("3260", "MED-HEALTH DIAGNOSING AND TREATING PRACTITIONERS, ALL OTHER"),
        R("3300", "MED-CLINICAL LABORATORY TECHNOLOGISTS AND TECHNICIANS"), R("3310", "MED-DENTAL HYGIENISTS"),
        R("3320", "MED-DIAGNOSTIC RELATED TECHNOLOGISTS AND TECHNICIANS"),
        R("3400", "MED-EMERGENCY MEDICAL TECHNICIANS AND PARAMEDICS"),
        R("3410", "MED-HEALTH DIAGNOSING AND TREATING PRACTITIONER SUPPORT TECHNICIANS"),
        R("3500", "MED-LICENSED PRACTICAL AND LICENSED VOCATIONAL NURSES"),
        R("3510", "MED-MEDICAL RECORDS AND HEALTH INFORMATION TECHNICIANS"), R("3520", "MED-OPTICIANS, DISPENSING"),
        R("3530", "MED-MISCELLANEOUS HEALTH TECHNOLOGISTS AND TECHNICIANS"),
        R("3540", "MED-OTHER HEALTHCARE PRACTITIONERS AND TECHNICAL OCCUPATIONS"),
        R("3600", "HLS-NURSING, PSYCHIATRIC, AND HOME HEALTH AIDES"),
        R("3610", "HLS-OCCUPATIONAL THERAPIST ASSISTANTS AND AIDES"),
        R("3620", "HLS-PHYSICAL THERAPIST ASSISTANTS AND AIDES"), R("3630", "HLS-MASSAGE THERAPISTS"),
        R("3640", "HLS-DENTAL ASSISTANTS"),
        R("3650", "HLS-MEDICAL ASSISTANTS AND OTHER HEALTHCARE SUPPORT OCCUPATIONS, EXCEPT DENTAL ASSISTANTS"),
        R("3700", "PRT-FIRST-LINE SUPERVISORS/MANAGERS OF CORRECTIONAL OFFICERS"),
        R("3710", "PRT-FIRST-LINE SUPERVISORS/MANAGERS OF POLICE AND DETECTIVES"),
        R("3720", "PRT-FIRST-LINE SUPERVISORS/MANAGERS OF FIRE FIGHTING AND PREVENTION WORKERS"),
        R("3730", "PRT-SUPERVISORS, PROTECTIVE SERVICE WORKERS, ALL OTHER"), R("3740", "PRT-FIRE FIGHTERS"),
        R("3750", "PRT-FIRE INSPECTORS"), R("3800", "PRT-BAILIFFS, CORRECTIONAL OFFICERS, AND JAILERS"),
        R("3820", "PRT-DETECTIVES AND CRIMINAL INVESTIGATORS"), R("3840", "PRT-MISCELLANEOUS LAW ENFORCEMENT WORKERS"),
        R("3850", "PRT-POLICE OFFICERS"), R("3900", "PRT-ANIMAL CONTROL WORKERS"),
        R("3910", "PRT-PRIVATE DETECTIVES AND INVESTIGATORS"),
        R("3920", "PRT-SECURITY GUARDS AND GAMING SURVEILLANCE OFFICERS"), R("3940", "PRT-CROSSING GUARDS"),
        R("3950", "PRT-LIFEGUARDS AND OTHER PROTECTIVE SERVICE WORKERS"), R("4000", "EAT-CHEFS AND HEAD COOKS"),
        R("4010", "EAT-FIRST-LINE SUPERVISORS/MANAGERS OF FOOD PREPARATION AND SERVING WORKERS"),
        R("4020", "EAT-COOKS"), R("4030", "EAT-FOOD PREPARATION WORKERS"), R("4040", "EAT-BARTENDERS"),
        R("4050", "EAT-COMBINED FOOD PREPARATION AND SERVING WORKERS, INCLUDING FAST FOOD"),
        R("4060", "EAT-COUNTER ATTENDANTS, CAFETERIA, FOOD CONCESSION, AND COFFEE SHOP"),
        R("4110", "EAT-WAITERS AND WAITRESSES"), R("4120", "EAT-FOOD SERVERS, NONRESTAURANT"),
        R("4130",
            "EAT-MISCELLANEOUS FOOD PREPARATION AND SERVING RELATED WORKERS, INCLUDING DINING ROOM AND CAFETERIA ATTENDANTS AND BARTENDER HELPERS"),
        R("4140", "EAT-DISHWASHERS"), R("4150", "EAT-HOSTS AND HOSTESSES, RESTAURANT, LOUNGE, AND COFFEE SHOP"),
        R("4200", "CLN-FIRST-LINE SUPERVISORS/MANAGERS OF HOUSEKEEPING AND JANITORIAL WORKERS"),
        R("4210", "CLN-FIRST-LINE SUPERVISORS/MANAGERS OF LANDSCAPING, LAWN SERVICE, AND GROUNDSKEEPING WORKERS"),
        R("4220", "CLN-JANITORS AND BUILDING CLEANERS"), R("4230", "CLN-MAIDS AND HOUSEKEEPING CLEANERS"),
        R("4240", "CLN-PEST CONTROL WORKERS"), R("4250", "CLN-GROUNDS MAINTENANCE WORKERS"),
        R("4300", "PRS-FIRST-LINE SUPERVISORS/MANAGERS OF GAMING WORKERS"),
        R("4320", "PRS-FIRST-LINE SUPERVISORS/MANAGERS OF PERSONAL SERVICE WORKERS"), R("4340", "PRS-ANIMAL TRAINERS"),
        R("4350", "PRS-NONFARM ANIMAL CARETAKERS"), R("4400", "PRS-GAMING SERVICES WORKERS"),
        R("4410", "PRS-MOTION PICTURE PROJECTIONISTS"), R("4420", "PRS-USHERS, LOBBY ATTENDANTS, AND TICKET TAKERS"),
        R("4430", "PRS-MISCELLANEOUS ENTERTAINMENT ATTENDANTS AND RELATED WORKERS"),
        R("4460", "PRS-FUNERAL SERVICE WORKERS"), R("4500", "PRS-BARBERS"),
        R("4510", "PRS-HAIRDRESSERS, HAIRSTYLISTS, AND COSMETOLOGISTS"),
        R("4520", "PRS-MISCELLANEOUS PERSONAL APPEARANCE WORKERS"),
        R("4530", "PRS-BAGGAGE PORTERS, BELLHOPS, AND CONCIERGES"), R("4540", "PRS-TOUR AND TRAVEL GUIDES"),
        R("4550", "PRS-TRANSPORTATION ATTENDANTS"), R("4600", "PRS-CHILD CARE WORKERS"),
        R("4610", "PRS-PERSONAL AND HOME CARE AIDES"), R("4620", "PRS-RECREATION AND FITNESS WORKERS"),
        R("4640", "PRS-RESIDENTIAL ADVISORS"), R("4650", "PRS-PERSONAL CARE AND SERVICE WORKERS, ALL OTHER"),
        R("4700", "SAL-FIRST-LINE SUPERVISORS/MANAGERS OF RETAIL SALES WORKERS"),
        R("4710", "SAL-FIRST-LINE SUPERVISORS/MANAGERS OF NON-RETAIL SALES WORKERS"), R("4720", "SAL-CASHIERS"),
        R("4740", "SAL-COUNTER AND RENTAL CLERKS"), R("4750", "SAL-PARTS SALESPERSONS"),
        R("4760", "SAL-RETAIL SALESPERSONS"), R("4800", "SAL-ADVERTISING SALES AGENTS"),
        R("4810", "SAL-INSURANCE SALES AGENTS"),
        R("4820", "SAL-SECURITIES, COMMODITIES, AND FINANCIAL SERVICES SALES AGENTS"), R("4830", "SAL-TRAVEL AGENTS"),
        R("4840", "SAL-SALES REPRESENTATIVES, SERVICES, ALL OTHER"),
        R("4850", "SAL-SALES REPRESENTATIVES, WHOLESALE AND MANUFACTURING"),
        R("4900", "SAL-MODELS, DEMONSTRATORS, AND PRODUCT PROMOTERS"),
        R("4920", "SAL-REAL ESTATE BROKERS AND SALES AGENTS"), R("4930", "SAL-SALES ENGINEERS"),
        R("4940", "SAL-TELEMARKETERS"),
        R("4950", "SAL-DOOR-TO-DOOR SALES WORKERS, NEWS AND STREET VENDORS, AND RELATED WORKERS"),
        R("4960", "SAL-SALES AND RELATED WORKERS, ALL OTHER"),
        R("5000", "OFF-FIRST-LINE SUPERVISORS/MANAGERS OF OFFICE AND ADMINISTRATIVE SUPPORT WORKERS"),
        R("5010", "OFF-SWITCHBOARD OPERATORS, INCLUDING ANSWERING SERVICE"), R("5020", "OFF-TELEPHONE OPERATORS"),
        R("5030", "OFF-COMMUNICATIONS EQUIPMENT OPERATORS, ALL OTHER"), R("5100", "OFF-BILL AND ACCOUNT COLLECTORS"),
        R("5110", "OFF-BILLING AND POSTING CLERKS AND MACHINE OPERATORS"),
        R("5120", "OFF-BOOKKEEPING, ACCOUNTING, AND AUDITING CLERKS"), R("5130", "OFF-GAMING CAGE WORKERS"),
        R("5140", "OFF-PAYROLL AND TIMEKEEPING CLERKS"), R("5150", "OFF-PROCUREMENT CLERKS"), R("5160", "OFF-TELLERS"),
        R("5200", "OFF-BROKERAGE CLERKS"), R("5220", "OFF-COURT, MUNICIPAL, AND LICENSE CLERKS"),
        R("5230", "OFF-CREDIT AUTHORIZERS, CHECKERS, AND CLERKS"), R("5240", "OFF-CUSTOMER SERVICE REPRESENTATIVES"),
        R("5250", "OFF-ELIGIBILITY INTERVIEWERS, GOVERNMENT PROGRAMS"), R("5260", "OFF-FILE CLERKS"),
        R("5300", "OFF-HOTEL, MOTEL, AND RESORT DESK CLERKS"),
        R("5310", "OFF-INTERVIEWERS, EXCEPT ELIGIBILITY AND LOAN"), R("5320", "OFF-LIBRARY ASSISTANTS, CLERICAL"),
        R("5330", "OFF-LOAN INTERVIEWERS AND CLERKS"), R("5340", "OFF-NEW ACCOUNTS CLERKS"),
        R("5350", "OFF-CORRESPONDENCE CLERKS AND ORDER CLERKS"),
        R("5360", "OFF-HUMAN RESOURCES ASSISTANTS, EXCEPT PAYROLL AND TIMEKEEPING"),
        R("5400", "OFF-RECEPTIONISTS AND INFORMATION CLERKS"),
        R("5410", "OFF-RESERVATION AND TRANSPORTATION TICKET AGENTS AND TRAVEL CLERKS"),
        R("5420", "OFF-INFORMATION AND RECORD CLERKS, ALL OTHER"), R("5500", "OFF-CARGO AND FREIGHT AGENTS"),
        R("5510", "OFF-COURIERS AND MESSENGERS"), R("5520", "OFF-DISPATCHERS"),
        R("5530", "OFF-METER READERS, UTILITIES"), R("5540", "OFF-POSTAL SERVICE CLERKS"),
        R("5550", "OFF-POSTAL SERVICE MAIL CARRIERS"),
        R("5560", "OFF-POSTAL SERVICE MAIL SORTERS, PROCESSORS, AND PROCESSING MACHINE OPERATORS"),
        R("5600", "OFF-PRODUCTION, PLANNING, AND EXPEDITING CLERKS"),
        R("5610", "OFF-SHIPPING, RECEIVING, AND TRAFFIC CLERKS"), R("5620", "OFF-STOCK CLERKS AND ORDER FILLERS"),
        R("5630", "OFF-WEIGHERS, MEASURERS, CHECKERS, AND SAMPLERS, RECORDKEEPING"),
        R("5700", "OFF-SECRETARIES AND ADMINISTRATIVE ASSISTANTS"), R("5800", "OFF-COMPUTER OPERATORS"),
        R("5810", "OFF-DATA ENTRY KEYERS"), R("5820", "OFF-WORD PROCESSORS AND TYPISTS"),
        R("5840", "OFF-INSURANCE CLAIMS AND POLICY PROCESSING CLERKS"),
        R("5850", "OFF-MAIL CLERKS AND MAIL MACHINE OPERATORS, EXCEPT POSTAL SERVICE"),
        R("5860", "OFF-OFFICE CLERKS, GENERAL"), R("5900", "OFF-OFFICE MACHINE OPERATORS, EXCEPT COMPUTER"),
        R("5910", "OFF-PROOFREADERS AND COPY MARKERS"), R("5920", "OFF-STATISTICAL ASSISTANTS"),
        R("5930", "OFF-MISCELLANEOUS OFFICE AND ADMINISTRATIVE SUPPORT WORKERS, INCLUDING DESKTOP PUBLISHERS"),
        R("6000", "FFF-FIRST-LINE SUPERVISORS/MANAGERS OF FARMING, FISHING, AND FORESTRY WORKERS"),
        R("6010", "FFF-AGRICULTURAL INSPECTORS"), R("6040", "FFF-GRADERS AND SORTERS, AGRICULTURAL PRODUCTS"),
        R("6050", "FFF-MISCELLANEOUS AGRICULTURAL WORKERS, INCLUDING ANIMAL BREEDERS"),
        R("6100", "FFF-FISHING AND HUNTING WORKERS"), R("6120", "FFF-FOREST AND CONSERVATION WORKERS"),
        R("6130", "FFF-LOGGING WORKERS"),
        R("6200", "CON-FIRST-LINE SUPERVISORS/MANAGERS OF CONSTRUCTION TRADES AND EXTRACTION WORKERS"),
        R("6210", "CON-BOILERMAKERS"), R("6220", "CON-BRICKMASONS, BLOCKMASONS, AND STONEMASONS"),
        R("6230", "CON-CARPENTERS"), R("6240", "CON-CARPET, FLOOR, AND TILE INSTALLERS AND FINISHERS"),
        R("6250", "CON-CEMENT MASONS, CONCRETE FINISHERS, AND TERRAZZO WORKERS"),
        R("6260", "CON-CONSTRUCTION LABORERS"), R("6300", "CON-PAVING, SURFACING, AND TAMPING EQUIPMENT OPERATORS"),
        R("6320", "CON-CONSTRUCTION EQUIPMENT OPERATORS, EXCEPT PAVING, SURFACING, AND TAMPING EQUIPMENT OPERATORS"),
        R("6330", "CON-DRYWALL INSTALLERS, CEILING TILE INSTALLERS, AND TAPERS"), R("6350", "CON-ELECTRICIANS"),
        R("6360", "CON-GLAZIERS"), R("6400", "CON-INSULATION WORKERS"),
        R("6420", "CON-PAINTERS, CONSTRUCTION AND MAINTENANCE"), R("6430", "CON-PAPERHANGERS"),
        R("6440", "CON-PIPELAYERS, PLUMBERS, PIPEFITTERS, AND STEAMFITTERS"),
        R("6460", "CON-PLASTERERS AND STUCCO MASONS"), R("6500", "CON-REINFORCING IRON AND REBAR WORKERS"),
        R("6510", "CON-ROOFERS"), R("6520", "CON-SHEET METAL WORKERS"),
        R("6530", "CON-STRUCTURAL IRON AND STEEL WORKERS"), R("6600", "CON-HELPERS, CONSTRUCTION TRADES"),
        R("6660", "CON-CONSTRUCTION AND BUILDING INSPECTORS"), R("6700", "CON-ELEVATOR INSTALLERS AND REPAIRERS"),
        R("6710", "CON-FENCE ERECTORS"), R("6720", "CON-HAZARDOUS MATERIALS REMOVAL WORKERS"),
        R("6730", "CON-HIGHWAY MAINTENANCE WORKERS"),
        R("6740", "CON-RAIL-TRACK LAYING AND MAINTENANCE EQUIPMENT OPERATORS"),
        R("6760", "CON-MISCELLANEOUS CONSTRUCTION WORKERS, INCLUDING SEPTIC TANK SERVICERS AND SEWER PIPE CLEANERS"),
        R("6800", "EXT-DERRICK, ROTARY DRILL, AND SERVICE UNIT OPERATORS, AND ROUSTABOUTS, OIL, GAS, AND MINING"),
        R("6820", "EXT-EARTH DRILLERS, EXCEPT OIL AND GAS"),
        R("6830", "EXT-EXPLOSIVES WORKERS, ORDNANCE HANDLING EXPERTS, AND BLASTERS"),
        R("6840", "EXT-MINING MACHINE OPERATORS"),
        R("6940", "EXT-MISCELLANEOUS EXTRACTION WORKERS, INCLUDING ROOF BOLTERS AND HELPERS"),
        R("7000", "RPR-FIRST-LINE SUPERVISORS/MANAGERS OF MECHANICS, INSTALLERS, AND REPAIRERS"),
        R("7010", "RPR-COMPUTER, AUTOMATED TELLER, AND OFFICE MACHINE REPAIRERS"),
        R("7020", "RPR-RADIO AND TELECOMMUNICATIONS EQUIPMENT INSTALLERS AND REPAIRERS"),
        R("7030", "RPR-AVIONICS TECHNICIANS"), R("7040", "RPR-ELECTRIC MOTOR, POWER TOOL, AND RELATED REPAIRERS"),
        R("7100", "RPR-ELECTRICAL AND ELECTRONICS REPAIRERS, TRANSPORTATION EQUIPMENT, AND INDUSTRIAL AND UTILITY"),
        R("7110", "RPR-ELECTRONIC EQUIPMENT INSTALLERS AND REPAIRERS, MOTOR VEHICLES"),
        R("7120", "RPR-ELECTRONIC HOME ENTERTAINMENT EQUIPMENT INSTALLERS AND REPAIRERS"),
        R("7130", "RPR-SECURITY AND FIRE ALARM SYSTEMS INSTALLERS"),
        R("7140", "RPR-AIRCRAFT MECHANICS AND SERVICE TECHNICIANS"),
        R("7150", "RPR-AUTOMOTIVE BODY AND RELATED REPAIRERS"),
        R("7160", "RPR-AUTOMOTIVE GLASS INSTALLERS AND REPAIRERS"),
        R("7200", "RPR-AUTOMOTIVE SERVICE TECHNICIANS AND MECHANICS"),
        R("7210", "RPR-BUS AND TRUCK MECHANICS AND DIESEL ENGINE SPECIALISTS"),
        R("7220", "RPR-HEAVY VEHICLE AND MOBILE EQUIPMENT SERVICE TECHNICIANS AND MECHANICS"),
        R("7240", "RPR-SMALL ENGINE MECHANICS"),
        R("7260", "RPR-MISCELLANEOUS VEHICLE AND MOBILE EQUIPMENT MECHANICS, INSTALLERS, AND REPAIRERS"),
        R("7300", "RPR-CONTROL AND VALVE INSTALLERS AND REPAIRERS"),
        R("7310", "RPR-HEATING, AIR CONDITIONING, AND REFRIGERATION MECHANICS AND INSTALLERS"),
        R("7320", "RPR-HOME APPLIANCE REPAIRERS"), R("7330", "RPR-INDUSTRIAL AND REFRACTORY MACHINERY MECHANICS"),
        R("7340", "RPR-MAINTENANCE AND REPAIR WORKERS, GENERAL"), R("7350", "RPR-MAINTENANCE WORKERS, MACHINERY"),
        R("7360", "RPR-MILLWRIGHTS"), R("7410", "RPR-ELECTRICAL POWER-LINE INSTALLERS AND REPAIRERS"),
        R("7420", "RPR-TELECOMMUNICATIONS LINE INSTALLERS AND REPAIRERS"),
        R("7430", "RPR-PRECISION INSTRUMENT AND EQUIPMENT REPAIRERS"),
        R("7510", "RPR-COIN, VENDING, AND AMUSEMENT MACHINE SERVICERS AND REPAIRERS"),
        R("7540", "RPR-LOCKSMITHS AND SAFE REPAIRERS"),
        R("7550", "RPR-MANUFACTURED BUILDING AND MOBILE HOME INSTALLERS"), R("7560", "RPR-RIGGERS"),
        R("7610", "RPR-HELPERS--INSTALLATION, MAINTENANCE, AND REPAIR WORKERS"),
        R("7620",
            "RPR-OTHER INSTALLATION, MAINTENANCE, AND REPAIR WORKERS, INCLUDING COMMERCIAL DIVERS, AND SIGNAL AND TRACK SWITCH REPAIRERS"),
        R("7700", "PRD-FIRST-LINE SUPERVISORS/MANAGERS OF PRODUCTION AND OPERATING WORKERS"),
        R("7710", "PRD-AIRCRAFT STRUCTURE, SURFACES, RIGGING, AND SYSTEMS ASSEMBLERS"),
        R("7720", "PRD-ELECTRICAL, ELECTRONICS, AND ELECTROMECHANICAL ASSEMBLERS"),
        R("7730", "PRD-ENGINE AND OTHER MACHINE ASSEMBLERS"), R("7740", "PRD-STRUCTURAL METAL FABRICATORS AND FITTERS"),
        R("7750", "PRD-MISCELLANEOUS ASSEMBLERS AND FABRICATORS"), R("7800", "PRD-BAKERS"),
        R("7810", "PRD-BUTCHERS AND OTHER MEAT, POULTRY, AND FISH PROCESSING WORKERS"),
        R("7830", "PRD-FOOD AND TOBACCO ROASTING, BAKING, AND DRYING MACHINE OPERATORS AND TENDERS"),
        R("7840", "PRD-FOOD BATCHMAKERS"), R("7850", "PRD-FOOD COOKING MACHINE OPERATORS AND TENDERS"),
        R("7900", "PRD-COMPUTER CONTROL PROGRAMMERS AND OPERATORS"),
        R("7920", "PRD-EXTRUDING AND DRAWING MACHINE SETTERS, OPERATORS, AND TENDERS, METAL AND PLASTIC"),
        R("7930", "PRD-FORGING MACHINE SETTERS, OPERATORS, AND TENDERS, METAL AND PLASTIC"),
        R("7940", "PRD-ROLLING MACHINE SETTERS, OPERATORS, AND TENDERS, METAL AND PLASTIC"),
        R("7950", "PRD-CUTTING, PUNCHING, AND PRESS MACHINE SETTERS, OPERATORS, AND TENDERS, METAL AND PLASTIC"),
        R("7960", "PRD-DRILLING AND BORING MACHINE TOOL SETTERS, OPERATORS, AND TENDERS, METAL AND PLASTIC"),
        R("8000",
            "PRD-GRINDING, LAPPING, POLISHING, AND BUFFING MACHINE TOOL SETTERS, OPERATORS, AND TENDERS, METAL AND PLASTIC"),
        R("8010", "PRD-LATHE AND TURNING MACHINE TOOL SETTERS, OPERATORS, AND TENDERS, METAL AND PLASTIC"),
        R("8030", "PRD-MACHINISTS"), R("8040", "PRD-METAL FURNACE AND KILN OPERATORS AND TENDERS"),
        R("8060", "PRD-MODEL MAKERS AND PATTERNMAKERS, METAL AND PLASTIC"),
        R("8100", "PRD-MOLDERS AND MOLDING MACHINE SETTERS, OPERATORS, AND TENDERS, METAL AND PLASTIC"),
        R("8130", "PRD-TOOL AND DIE MAKERS"), R("8140", "PRD-WELDING, SOLDERING, AND BRAZING WORKERS"),
        R("8150", "PRD-HEAT TREATING EQUIPMENT SETTERS, OPERATORS, AND TENDERS, METAL AND PLASTIC"),
        R("8200", "PRD-PLATING AND COATING MACHINE SETTERS, OPERATORS, AND TENDERS, METAL AND PLASTIC"),
        R("8210", "PRD-TOOL GRINDERS, FILERS, AND SHARPENERS"),
        R("8220",
            "PRD-MISCELLANEOUS METAL WORKERS AND PLASTIC WORKERS, INCLUDING MILLING AND PLANING MACHINE SETTERS, AND MULTIPLE MACHINE TOOL SETTERS, AND LAY-OUT WORKERS"),
        R("8230", "PRD-BOOKBINDERS AND BINDERY WORKERS"), R("8240", "PRD-JOB PRINTERS"),
        R("8250", "PRD-PREPRESS TECHNICIANS AND WORKERS"), R("8260", "PRD-PRINTING MACHINE OPERATORS"),
        R("8300", "PRD-LAUNDRY AND DRY-CLEANING WORKERS"),
        R("8310", "PRD-PRESSERS, TEXTILE, GARMENT, AND RELATED MATERIALS"), R("8320", "PRD-SEWING MACHINE OPERATORS"),
        R("8330", "PRD-SHOE AND LEATHER WORKERS AND REPAIRERS"), R("8340", "PRD-SHOE MACHINE OPERATORS AND TENDERS"),
        R("8350", "PRD-TAILORS, DRESSMAKERS, AND SEWERS"),
        R("8400", "PRD-TEXTILE BLEACHING AND DYEING, AND CUTTING MACHINE SETTERS, OPERATORS, AND TENDERS"),
        R("8410", "PRD-TEXTILE KNITTING AND WEAVING MACHINE SETTERS, OPERATORS, AND TENDERS"),
        R("8420", "PRD-TEXTILE WINDING, TWISTING, AND DRAWING OUT MACHINE SETTERS, OPERATORS, AND TENDERS"),
        R("8450", "PRD-UPHOLSTERERS"),
        R("8460", "PRD-MISCELLANEOUS TEXTILE, APPAREL, AND FURNISHINGS WORKERS, EXCEPT UPHOLSTERERS"),
        R("8500", "PRD-CABINETMAKERS AND BENCH CARPENTERS"), R("8510", "PRD-FURNITURE FINISHERS"),
        R("8530", "PRD-SAWING MACHINE SETTERS, OPERATORS, AND TENDERS, WOOD"),
        R("8540", "PRD-WOODWORKING MACHINE SETTERS, OPERATORS, AND TENDERS, EXCEPT SAWING"),
        R("8550", "PRD-MISCELLANEOUS WOODWORKERS, INCLUDING MODEL MAKERS AND PATTERNMAKERS"),
        R("8600", "PRD-POWER PLANT OPERATORS, DISTRIBUTORS, AND DISPATCHERS"),
        R("8610", "PRD-STATIONARY ENGINEERS AND BOILER OPERATORS"),
        R("8620", "PRD-WATER AND LIQUID WASTE TREATMENT PLANT AND SYSTEM OPERATORS"),
        R("8630", "PRD-MISCELLANEOUS PLANT AND SYSTEM OPERATORS"),
        R("8640", "PRD-CHEMICAL PROCESSING MACHINE SETTERS, OPERATORS, AND TENDERS"),
        R("8650", "PRD-CRUSHING, GRINDING, POLISHING, MIXING, AND BLENDING WORKERS"), R("8710", "PRD-CUTTING WORKERS"),
        R("8720", "PRD-EXTRUDING, FORMING, PRESSING, AND COMPACTING MACHINE SETTERS, OPERATORS, AND TENDERS"),
        R("8730", "PRD-FURNACE, KILN, OVEN, DRIER, AND KETTLE OPERATORS AND TENDERS"),
        R("8740", "PRD-INSPECTORS, TESTERS, SORTERS, SAMPLERS, AND WEIGHERS"),
        R("8750", "PRD-JEWELERS AND PRECIOUS STONE AND METAL WORKERS"),
        R("8760", "PRD-MEDICAL, DENTAL, AND OPHTHALMIC LABORATORY TECHNICIANS"),
        R("8800", "PRD-PACKAGING AND FILLING MACHINE OPERATORS AND TENDERS"), R("8810", "PRD-PAINTING WORKERS"),
        R("8830", "PRD-PHOTOGRAPHIC PROCESS WORKERS AND PROCESSING MACHINE OPERATORS"),
        R("8850", "PRD-CEMENTING AND GLUING MACHINE OPERATORS AND TENDERS"),
        R("8860", "PRD-CLEANING, WASHING, AND METAL PICKLING EQUIPMENT OPERATORS AND TENDERS"),
        R("8910", "PRD-ETCHERS AND ENGRAVERS"),
        R("8920", "PRD-MOLDERS, SHAPERS, AND CASTERS, EXCEPT METAL AND PLASTIC"),
        R("8930", "PRD-PAPER GOODS MACHINE SETTERS, OPERATORS, AND TENDERS"), R("8940", "PRD-TIRE BUILDERS"),
        R("8950", "PRD-HELPERS-PRODUCTION WORKERS"),
        R("8960",
            "PRD-OTHER PRODUCTION WORKERS, INCLUDING SEMICONDUCTOR PROCESSORS AND COOLING AND FREEZING EQUIPMENT OPERATORS"),
        R("9000", "TRN-SUPERVISORS, TRANSPORTATION AND MATERIAL MOVING WORKERS"),
        R("9030", "TRN-AIRCRAFT PILOTS AND FLIGHT ENGINEERS"),
        R("9040", "TRN-AIR TRAFFIC CONTROLLERS AND AIRFIELD OPERATIONS SPECIALISTS"),
        R("9110", "TRN-AMBULANCE DRIVERS AND ATTENDANTS, EXCEPT EMERGENCY MEDICAL TECHNICIANS"),
        R("9120", "TRN-BUS DRIVERS"), R("9130", "TRN-DRIVER/SALES WORKERS AND TRUCK DRIVERS"),
        R("9140", "TRN-TAXI DRIVERS AND CHAUFFEURS"), R("9150", "TRN-MOTOR VEHICLE OPERATORS, ALL OTHER"),
        R("9200", "TRN-LOCOMOTIVE ENGINEERS AND OPERATORS"),
        R("9230", "TRN-RAILROAD BRAKE, SIGNAL, AND SWITCH OPERATORS"),
        R("9240", "TRN-RAILROAD CONDUCTORS AND YARDMASTERS"),
        R("9260", "TRN-SUBWAY, STREETCAR, AND OTHER RAIL TRANSPORTATION WORKERS"),
        R("9300", "TRN-SAILORS AND MARINE OILERS, AND SHIP ENGINEERS"),
        R("9310", "TRN-SHIP AND BOAT CAPTAINS AND OPERATORS"), R("9350", "TRN-PARKING LOT ATTENDANTS"),
        R("9360", "TRN-SERVICE STATION ATTENDANTS"), R("9410", "TRN-TRANSPORTATION INSPECTORS"),
        R("9420",
            "TRN-MISCELLANEOUS TRANSPORTATION WORKERS, INCLUDING BRIDGE AND LOCK TENDERS AND TRAFFIC TECHNICIANS"),
        R("9510", "TRN-CRANE AND TOWER OPERATORS"), R("9520", "TRN-DREDGE, EXCAVATING, AND LOADING MACHINE OPERATORS"),
        R("9560", "TRN-CONVEYOR OPERATORS AND TENDERS, AND HOIST AND WINCH OPERATORS"),
        R("9600", "TRN-INDUSTRIAL TRUCK AND TRACTOR OPERATORS"), R("9610", "TRN-CLEANERS OF VEHICLES AND EQUIPMENT"),
        R("9620", "TRN-LABORERS AND FREIGHT, STOCK, AND MATERIAL MOVERS, HAND"),
        R("9630", "TRN-MACHINE FEEDERS AND OFFBEARERS"), R("9640", "TRN-PACKERS AND PACKAGERS, HAND"),
        R("9650", "TRN-PUMPING STATION OPERATORS"), R("9720", "TRN-REFUSE AND RECYCLABLE MATERIAL COLLECTORS"),
        R("9750",
            "TRN-MISCELLANEOUS MATERIAL MOVING WORKERS, INCLUDING SHUTTLE CAR OPERATORS, AND TANK CAR, TRUCK, AND SHIP LOADERS"),
        R("9800", "MIL-MILITARY OFFICER SPECIAL AND TACTICAL OPERATIONS LEADERS/MANAGERS"),
        R("9810", "MIL-FIRST-LINE ENLISTED MILITARY SUPERVISORS/MANAGERS"),
        R("9820", "MIL-MILITARY ENLISTED TACTICAL OPERATIONS AND AIR/WEAPONS SPECIALISTS AND CREW MEMBERS"),
        R("9830", "MIL-MILITARY, RANK NOT SPECIFIED "),
        R("9920", "UNEMPLOYED AND LAST WORKED 5 YEARS AGO OR EARLIER OR NEVER WORKED"))));
    colInfo.put("OCPIP", new Triple<>("owner_cost_monthly_percentage_household_income", ColumnType.LONG, longSpecial(//
        R("bbb", -1L))));
    colInfo.put("OIP", new Triple<>("other_income_12_months", ColumnType.LONG, longSpecial(//
        R("bbbbbb", -1L))));
    colInfo.put("PAOC",
        new Triple<>("presence_and_age_of_own_children", ColumnType.STRING,
            new ReplaceFn(//
                R("b", "n/a"), R("1", "With own children under 6 years only"),
                R("2", "With own children 6 to 17 years only"),
                R("3", "With own children under 6 years and 6 to 17 years only"), R("4", "No own children"))));
    colInfo.put("PAP", new Triple<>("public_assistence_income_12_months", ColumnType.LONG, longSpecial(//
        R("bbbbb", -1L))));
    colInfo.put("PARTNER",
        new Triple<>("unmarried_partner_household", ColumnType.STRING,
            new ReplaceFn(//
                R("b", "n/a"), R("0", "No unmarried partner in household"), R("1", "Male householder, male partner"),
                R("2", "Male householder, female partner"), R("3", "Female householder, female partner"),
                R("4", "Female householder, male partner"))));
    colInfo.put("PERNP", new Triple<>("total_earnings", ColumnType.LONG, longSpecial(//
        R("bbbbbbb", 0L))));
    colInfo.put("PINCP", new Triple<>("total_income", ColumnType.LONG, longSpecial(//
        R("bbbbbbb", 0L))));
    colInfo.put("PLM", new Triple<>("complete_plumbing", ColumnType.LONG, longSpecial(//
        R("b", -1L), R("1", 1L), R("2", 0L))));
    colInfo.put("POBP", new Triple<>("place_of_birth", ColumnType.STRING, new ReplaceFn(//
        R("001", "Alabama/AL"), R("002", "Alaska/AK"), R("004", "Arizona/AZ"), R("005", "Arkansas/AR"),
        R("006", "California/CA"), R("008", "Colorado/CO"), R("009", "Connecticut/CT"), R("010", "Delaware/DE"),
        R("011", "District of Columbia/DC"), R("012", "Florida/FL"), R("013", "Georgia/GA"), R("015", "Hawaii/HI"),
        R("016", "Idaho/ID"), R("017", "Illinois/IL"), R("018", "Indiana/IN"), R("019", "Iowa/IA"),
        R("020", "Kansas/KS"), R("021", "Kentucky/KY"), R("022", "Louisiana/LA"), R("023", "Maine/ME"),
        R("024", "Maryland/MD"), R("025", "Massachusetts/MA"), R("026", "Michigan/MI"), R("027", "Minnesota/MN"),
        R("028", "Mississippi/MS"), R("029", "Missouri/MO"), R("030", "Montana/MT"), R("031", "Nebraska/NE"),
        R("032", "Nevada/NV"), R("033", "New Hampshire/NH"), R("034", "New Jersey/NJ"), R("035", "New Mexico/NM"),
        R("036", "New York/NY"), R("037", "North Carolina/NC"), R("038", "North Dakota/ND"), R("039", "Ohio/OH"),
        R("040", "Oklahoma/OK"), R("041", "Oregon/OR"), R("042", "Pennsylvania/PA"), R("044", "Rhode Island/RI"),
        R("045", "South Carolina/SC"), R("046", "South Dakota/SD"), R("047", "Tennessee/TN"), R("048", "Texas/TX"),
        R("049", "Utah/UT"), R("050", "Vermont/VT"), R("051", "Virginia/VA"), R("053", "Washington/WA"),
        R("054", "West Virginia/WV"), R("055", "Wisconsin/WI"), R("056", "Wyoming/WY"), R("060", "American Samoa"),
        R("066", "Guam"), R("072", "Puerto Rico"), R("078", "US Virgin Islands"), R("100", "Albania"),
        R("102", "Austria"), R("103", "Belgium"), R("104", "Bulgaria"), R("105", "Czechoslovakia"), R("106", "Denmark"),
        R("108", "Finland"), R("109", "France"), R("110", "Germany"), R("116", "Greece"), R("117", "Hungary"),
        R("118", "Iceland"), R("119", "Ireland"), R("120", "Italy"), R("126", "Netherlands"), R("127", "Norway"),
        R("128", "Poland"), R("129", "Portugal"), R("130", "Azores Islands"), R("132", "Romania"), R("134", "Spain"),
        R("136", "Sweden"), R("137", "Switzerland"), R("138", "United Kingdom, Not Specified"), R("139", "England"),
        R("140", "Scotland"), R("142", "Northern Ireland"), R("147", "Yugoslavia"), R("148", "Czech Republic"),
        R("149", "Slovakia"), R("150", "Bosnia and Herzegovina"), R("151", "Croatia"), R("152", "Macedonia"),
        R("155", "Estonia"), R("156", "Latvia"), R("157", "Lithuania"), R("158", "Armenia"), R("159", "Azerbaijan"),
        R("160", "Belarus"), R("161", "Georgia"), R("162", "Moldova"), R("163", "Russia"), R("164", "Ukraine"),
        R("165", "USSR"), R("166", "Europe, Not Specified"), R("169", "Other Europe, Not Specified"),
        R("200", "Afghanistan"), R("202", "Bangladesh"), R("205", "Burma"), R("206", "Cambodia"), R("207", "China"),
        R("209", "Hong Kong"), R("210", "India"), R("211", "Indonesia"), R("212", "Iran"), R("213", "Iraq"),
        R("214", "Israel"), R("215", "Japan"), R("216", "Jordan"), R("217", "Korea"), R("218", "Kazakhstan"),
        R("222", "Kuwait"), R("223", "Laos"), R("224", "Lebanon"), R("226", "Malaysia"), R("229", "Nepal"),
        R("231", "Pakistan"), R("233", "Philippines"), R("235", "Saudi Arabia"), R("236", "Singapore"),
        R("238", "Sri Lanka"), R("239", "Syria"), R("240", "Taiwan"), R("242", "Thailand"), R("243", "Turkey"),
        R("246", "Uzbekistan"), R("247", "Vietnam"), R("248", "Yemen"), R("249", "Asia"),
        R("251", "Eastern Asia, Not Specified"), R("253", "Other South Central Asia, Not Specified"),
        R("254", "Other Asia, Not Specified"), R("300", "Bermuda"), R("301", "Canada"), R("303", "Mexico"),
        R("310", "Belize"), R("311", "Costa Rica"), R("312", "El Salvador"), R("313", "Guatemala"),
        R("314", "Honduras"), R("315", "Nicaragua"), R("316", "Panama"), R("321", "Antigua & Barbuda"),
        R("323", "Bahamas"), R("324", "Barbados"), R("327", "Cuba"), R("328", "Dominica"),
        R("329", "Dominican Republic"), R("330", "Grenada"), R("332", "Haiti"), R("333", "Jamaica"),
        R("338", "St. Kitts-Nevis"), R("339", "St. Lucia"), R("340", "St. Vincent & the Grenadines"),
        R("341", "Trinidad & Tobago"), R("343", "West Indies"), R("344", "Caribbean, Not Specified"),
        R("360", "Argentina"), R("361", "Bolivia"), R("362", "Brazil"), R("363", "Chile"), R("364", "Colombia"),
        R("365", "Ecuador"), R("368", "Guyana"), R("369", "Paraguay"), R("370", "Peru"), R("372", "Uruguay"),
        R("373", "Venezuela"), R("374", "South America"), R("399", "Americas, Not Specified"), R("400", "Algeria"),
        R("407", "Cameroon"), R("408", "Cape Verde"), R("414", "Egypt"), R("416", "Ethiopia"), R("417", "Eritrea"),
        R("421", "Ghana"), R("423", "Guinea"), R("427", "Kenya"), R("429", "Liberia"), R("436", "Morocco"),
        R("440", "Nigeria"), R("444", "Senegal"), R("447", "Sierra Leone"), R("448", "Somalia"),
        R("449", "South Africa"), R("451", "Sudan"), R("453", "Tanzania"), R("457", "Uganda"), R("461", "Zimbabwe"),
        R("462", "Africa"), R("463", "Eastern Africa, Not Specified"), R("464", "Northern Africa, Not Specified"),
        R("467", "Western Africa, Not Specified"), R("468", "Other Africa, Not Specified"), R("501", "Australia"),
        R("508", "Fiji"), R("512", "Micronesia"), R("515", "New Zealand"), R("523", "Tonga"), R("527", "Samoa"),
        R("554", "Other US Island Areas, Oceania, Not Specified, or at Sea"))));
    colInfo.put("POVPIP", new Triple<>("pverty_status", ColumnType.LONG, longSpecial(//
        R("bbb", -1L))));
    colInfo.put("POWPUMA", new Triple<>("place_of_work_puma", ColumnType.LONG, longSpecial(//
        R("bbbbb", -1L))));
    colInfo.put("POWSP",
        new Triple<>("place_of_work", ColumnType.STRING, new ReplaceFn(//
            R("bbb",
                "N/A (not a worker--not in the labor force, including persons under 16 years; unemployed; employed, with a job not at work; Armed Forces, with a job but not at work)"),
            R("001", "Alabama/AL"), R("002", "Alaska/AK"), R("004", "Arizona/AZ"), R("005", "Arkansas/AR"),
            R("006", "California/CA"), R("008", "Colorado/CO"), R("009", "Connecticut/CT"), R("010", "Delaware/DE"),
            R("011", "District of Columbia/DC"), R("012", "Florida/FL"), R("013", "Georgia/GA"), R("015", "Hawaii/HI"),
            R("016", "Idaho/ID"), R("017", "Illinois/IL"), R("018", "Indiana/IN"), R("019", "Iowa/IA"),
            R("020", "Kansas/KS"), R("021", "Kentucky/KY"), R("022", "Louisiana/LA"), R("023", "Maine/ME"),
            R("024", "Maryland/MD"), R("025", "Massachusetts/MA"), R("026", "Michigan/MI"), R("027", "Minnesota/MN"),
            R("028", "Mississippi/MS"), R("029", "Missouri/MO"), R("030", "Montana/MT"), R("031", "Nebraska/NE"),
            R("032", "Nevada/NV"), R("033", "New Hampshire/NH"), R("034", "New Jersey/NJ"), R("035", "New Mexico/NM"),
            R("036", "New York/NY"), R("037", "North Carolina/NC"), R("038", "North Dakota/ND"), R("039", "Ohio/OH"),
            R("040", "Oklahoma/OK"), R("041", "Oregon/OR"), R("042", "Pennsylvania/PA"), R("044", "Rhode Island/RI"),
            R("045", "South Carolina/SC"), R("046", "South Dakota/SD"), R("047", "Tennessee/TN"), R("048", "Texas/TX"),
            R("049", "Utah/UT"), R("050", "Vermont/VT"), R("051", "Virginia/VA"), R("053", "Washington/WA"),
            R("054", "West Virginia/WV"), R("055", "Wisconsin/WI"), R("056", "Wyoming/WY"), R("072", "Puerto Rico"),
            R("166", "Europe"), R("213", "Iraq"), R("251", "Eastern Asia"), R("254", "Other Asia, Not Specified"),
            R("303", "Mexico"), R("399", "Americas, Not Specified"),
            R("555", "Other US Island Areas Not Specified, Africa, Oceania, at Sea, or Abroad, Not Specified"))));
    colInfo.put("PSF", new Triple<>("subfamilies_in_household", ColumnType.LONG, longSpecial(//
        R("b", -1L), R("0", 0L), R("1", 1L))));
    colInfo.put("PUMA", new Triple<>("puma", ColumnType.LONG, longFn()));
    colInfo.put("PWGTP", new Triple<>("person_weight", ColumnType.LONG, longFn()));
    colInfo.put("PWGTPR", new Triple<>("person_weight_replicate", ColumnType.LONG, longFn()));
    colInfo.put("QTRBIR",
        new Triple<>("quater_of_birth", ColumnType.STRING,
            new ReplaceFn(//
                R("1", "January through March"), R("2", "April through June"), R("3", "July through September"),
                R("4", "October through December"))));
    colInfo.put("R18", new Triple<>("persons_under_18_in_household", ColumnType.LONG, longSpecial(//
        R("b", -1L), R("0", 0L), R("1", 1L))));
    colInfo.put("R60", new Triple<>("persons_over_60_in_household", ColumnType.LONG, longSpecial(//
        R("b", -1L), R("0", 0L), R("1", 1L))));
    colInfo.put("R65", new Triple<>("persons_over_65_in_household", ColumnType.LONG, longSpecial(//
        R("b", -1L), R("0", 0L), R("1", 1L))));
    colInfo.put("RAC1P",
        new Triple<>("detailed_race_code_recoded_1", ColumnType.STRING,
            new ReplaceFn(//
                R("1", "White alone"), R("2", "Black or African American alone"), R("3", "American Indian alone"),
                R("4", "Alaska Native alone"),
                R("5",
                    "American Indian and Alaska Native tribes specified; or American Indian or Alaska native, not specified and no other races"),
        R("6", "Asian alone"), R("7", "Native Hawaiian and Other Pacific Islander alone"),
        R("8", "Some other race alone"), R("9", "Two or more major race groups"))));
    colInfo.put("RAC2P",
        new Triple<>("detailed_race_code_recoded_2", ColumnType.STRING,
            new ReplaceFn(//
                R("01", "White alone"), R("02", "Black or African American alone"), R("03", "Apache alone"),
                R("04", "Blackfeet alone"), R("05", "Cherokee alone"), R("06", "Cheyenne alone"), R("07",
                    "Chickasaw alone"),
                R("08", "Chippewa alone"), R("09", "Choctaw alone"), R("10", "Colville alone"),
                R("11", "Comanche alone"), R("12", "Creek alone"), R("13", "Crow alone"), R("14", "Delaware alone"),
                R("15", "Houma alone"), R("16", "Iroquois alone"), R("17", "Lumbee alone"), R("18", "Menominee alone"),
                R("19", "Navajo alone"), R("20", "Paiute alone"), R("21", "Pima alone"), R("22", "Potawatomi alone"),
                R("23", "Pueblo alone"), R("24", "Puget Sound Salish alone"), R("25", "Seminole alone"),
                R("26", "Sioux alone"), R("27", "Tohono O'Odham alone"), R("28", "Yakama alone"),
                R("29", "Yaqui alone"), R("30", "Yuman alone"), R("31", "Other specified American Indian tribes alone"),
                R("32", "Combinations of American Indian tribes only"),
                R("33", "American Indian or Alaska Native, tribe not specified, or American Indian and Alaska Native"),
                R("34", "Alaskan Athabascan alone"), R("35", "Aleut alone"), R("36", "Eskimo alone"),
                R("37", "Tlingit-Haida alone"),
                R("38", "Alaska Native tribes alone or in combination with other Alaska Native tribes"),
                R("39", "American Indian or Alaska Native, not specified"), R("40", "Asian Indian alone"),
                R("41", "Bangladeshi alone"), R("42", "Cambodian alone"), R("43", "Chinese alone"),
                R("44", "Filipino alone"), R("45", "Hmong alone"), R("46", "Indonesian alone"),
                R("47", "Japanese alone"), R("48", "Korean alone"), R("49", "Laotian alone"),
                R("50", "Malaysian alone"), R("51", "Pakistani alone"), R("52", "Sri Lankan alone"),
                R("53", "Thai alone"), R("54", "Vietnamese alone"), R("55", "Other specified Asian alone"),
                R("56", "Asian, not specified"), R("57", "Combinations of Asian groups only"),
                R("58", "Native Hawaiian alone"), R("59", "Samoan alone"), R("60", "Tongan alone"),
                R("61", "Other Polynesian alone or in combination with other Polynesian groups"),
                R("62", "Guamanian or Chamorro alone"),
                R("63", "Other Micronesian alone or in combination with other Micronesian groups"),
                R("64", "Melanesian alone or in combination with other Melanesian groups"),
                R("65",
                    "Other Native Hawaiian and Other Pacific Islander groups alone or in combination with other Native Hawaiian and Other Pacific Islander groups only"),
        R("66", "Some other race alone"), R("67", "Two or more races"))));
    colInfo.put("RAC3P",
        new Triple<>("detailed_race_code_recoded_3", ColumnType.STRING, new ReplaceFn(//
            R("01", "Some other race alone"), R("02", "Other Pacific Islander alone"), R("03", "Samoan alone"),
            R("04", "Guamanian or Chamorro alone"), R("05", "Native Hawaiian alone"),
            R("06", "Native Hawaiian and Other Pacific Islander groups only"), R("07", "Other Asian; Some other race"),
            R("08", "Other Asian alone"), R("09", "Vietnamese alone"), R("10", "Korean alone"),
            R("11", "Japanese; Some other race"), R("12", "Japanese; Native Hawaiian"), R("13", "Japanese; Korean"),
            R("14", "Japanese alone"), R("15", "Filipino; Some other race"), R("16", "Filipino; Native Hawaiian"),
            R("17", "Filipino; Japanese"), R("18", "Filipino alone"), R("19", "Chinese; Some other race"),
            R("20", "Chinese; Native Hawaiian"), R("21", "Chinese; Other Asian"), R("22", "Chinese; Vietnamese"),
            R("23", "Chinese; Japanese"), R("24", "Chinese; Filipino; Native Hawaiian"), R("25", "Chinese; Filipino"),
            R("26", "Chinese alone"), R("27", "Asian Indian; Some other race"), R("28", "Asian Indian; Other Asian"),
            R("29", "Asian Indian alone"),
            R("30", "Asian groups and/or Native Hawaiian and Other Pacific Islander groups; Some other race"),
            R("31", "Asian groups; Native Hawaiian and Other Pacific Islander groups"), R("32", "Asian groups only"),
            R("33", "American Indian and Alaska Native; Some other race"),
            R("34", "American Indian and Alaska Native alone"),
            R("35",
                "American Indian and Alaska Native race; Asian groups and/or Native Hawaiian and Other Pacific Islander groups and/or Some other race"),
        R("36", "Black or African American; Some other race"), R("37", "Black or African American; Other Asian"),
        R("38", "Black or African American; Korean"), R("39", "Black or African American; Japanese"),
        R("40", "Black or African American; Filipino"), R("41", "Black or African American; Chinese"),
        R("42", "Black or African American; Asian Indian"),
        R("43", "Black or African American; American Indian and Alaska Native"),
        R("44", "Black or African American alone"),
        R("45", "Black or African American race; Native Hawaiian and Other Pacific Islander groups"),
        R("46",
            "Black or African American race; Asian groups and/or Native Hawaiian and Other Pacific Islander groups and/or Some other race"),
        R("47",
            "Black or African American race; American Indian and Alaska Native race; Asian groups and/or Native Hawaiian and Other Pacific Islander groups and/or Some other race"),
        R("48", "White; Some other race"), R("49", "White; Other Pacific Islander"), R("50", "White; Samoan"),
        R("51", "White; Guamanian or Chamorro"), R("52", "White; Native Hawaiian"), R("53", "White; Other Asian"),
        R("54", "White; Vietnamese"), R("55", "White; Korean"), R("56", "White; Japanese; Native Hawaiian"),
        R("57", "White; Japanese"), R("58", "White; Filipino; Native Hawaiian"), R("59", "White; Filipino"),
        R("60", "White; Chinese; Native Hawaiian"), R("61", "White; Chinese; Filipino; Native Hawaiian"),
        R("62", "White; Chinese"), R("63", "White; Asian Indian"),
        R("64", "White; American Indian and Alaska Native; Some other race"),
        R("65", "White; American Indian and Alaska Native"),
        R("66", "White; Black or African American; Some other race"),
        R("67", "White; Black or African American; American Indian and Alaska Native"),
        R("68", "White; Black or African American"), R("69", "White alone"),
        R("70", "White race; two or more Asian groups"),
        R("71",
            "White race; Black or African American race and/or American Indian and Alaska Native race and/or Asian groups and/or Native Hawaiian and Other Pacific Islander groups"),
        R("72",
            "White race; Some other race; Black or African American race and/or American Indian and Alaska Native race and/or Asian groups and/or Native Hawaiian and Other Pacific Islander groups"))));
    colInfo.put("RACAIAN", new Triple<>("american_indian_and_alaska_native_recode", ColumnType.LONG, bool()));
    colInfo.put("RACASN", new Triple<>("asian_recode", ColumnType.LONG, bool()));
    colInfo.put("RACBLK", new Triple<>("black_or_african_recode", ColumnType.LONG, bool()));
    colInfo.put("RACNHPI", new Triple<>("native_hawaiian_pacific_islander_recode", ColumnType.LONG, bool()));
    colInfo.put("RACNUM", new Triple<>("major_race_groups_represented", ColumnType.LONG, longFn()));
    colInfo.put("RACSOR", new Triple<>("some_other_race_recode", ColumnType.LONG, bool()));
    colInfo.put("RACWHT", new Triple<>("white_recode", ColumnType.LONG, bool()));
    colInfo.put("RC", new Triple<>("related_child", ColumnType.LONG, bool()));
    colInfo.put("REGION", new Triple<>("region", ColumnType.STRING, new ReplaceFn(//
        R("1", "Northeast"), R("2", "Midwest"), R("3", "South"), R("4", "West"), R("9", "Puerto Rico"))));
    colInfo.put("REL",
        new Triple<>("relationship", ColumnType.STRING,
            new ReplaceFn(//
                R("00", "Reference person"), R("01", "Husband/wife"), R("02", "Son/daughter"),
                R("03", "Brother/sister"), R("04", "Father/mother"), R("05", "Grandchild"), R("06", "In-law"),
                R("07", "Other relative"), R("08", "Roomer/boarder"), R("09", "Housemate/roommate"),
                R("10", "Unmarried partner"), R("11", "Foster child"), R("12", "Other nonrelative"),
                R("13", "Institutionalized group quarters population"),
                R("14", "Noninstitutionalized group quarters population"))));
    colInfo.put("RESMODE", new Triple<>("response_mode", ColumnType.STRING, new ReplaceFn(//
        R("b", "n/a"), R("1", "Mail"), R("2", "CATI/CAPI"))));
    colInfo.put("RETP", new Triple<>("retirement_income_12_months", ColumnType.LONG, longSpecial(//
        R("bbbbbb", -1L))));
    colInfo.put("RMS", new Triple<>("rooms", ColumnType.LONG, longSpecial(//
        R("b", -1L))));
    colInfo.put("RNTM", new Triple<>("meals_included_in_rent", ColumnType.LONG, longSpecial(//
        R("b", -1L), R("1", 1L), R("2", 0L))));
    colInfo.put("RNTP", new Triple<>("rent_monthly", ColumnType.LONG, longSpecial(//
        R("bbbbb", -1L))));
    colInfo.put("SCH",
        new Triple<>("school_enrollment", ColumnType.STRING,
            new ReplaceFn(//
                R("b", "n/a"), R("1", "No, has not attended in the last 3 months"),
                R("2", "Yes, public school or public college"), R("3", "Yes, private school or private college"))));
    colInfo.put("SCHG",
        new Triple<>("school_grade", ColumnType.STRING,
            new ReplaceFn(//
                R("b", "n/a"), R("1", "Nursery school/preschool"), R("2", "Kindergarten"), R("3", "Grade 1 to grade 4"),
                R("4", "Grade 5 to grade 8"), R("5", "Grade 9 to grade 12"), R("6", "College undergraduate"),
                R("7", "Graduate or professional school"))));
    colInfo.put("SCHL",
        new Triple<>("educational_attainment", ColumnType.STRING,
            new ReplaceFn(//
                R("bb", "N/A (less than 3 years old)"), R("01", "No schooling completed"),
                R("02", "Nursery school to grade 4"), R("03", "Grade 5 or grade 6"), R("04", "Grade 7 or grade 8"),
                R("05", "Grade 9"), R("06", "Grade 10"), R("07", "Grade 11"), R("08", "12th grade, no diploma"),
                R("09", "High school graduate"), R("10", "Some college, but less than 1 year"),
                R("11", "One or more years of college, no degree"), R("12", "Associate's degree"),
                R("13", "Bachelor's degree"), R("14", "Master's degree"), R("15", "Professional school degree"),
                R("16", "Doctorate degree"))));
    colInfo.put("SEMP", new Triple<>("self_employment_income_12_months", ColumnType.LONG, longSpecial(//
        R("bbbbbb", 0L))));
    colInfo.put("SEX", new Triple<>("sex", ColumnType.STRING, new ReplaceFn(//
        R("1", "male"), R("2", "female"))));
    colInfo.put("SFN", new Triple<>("subfamily_number", ColumnType.LONG, longSpecial(//
        R("b", -1L))));
    colInfo.put("SFR",
        new Triple<>("subfamily_relationship", ColumnType.STRING,
            new ReplaceFn(//
                R("b", "N/A (GQ/not in a subfamily)"), R("1", "Husband/wife no children"),
                R("2", "Husband/wife with children"), R("3", "Parent in a parent/child subfamily"),
                R("4", "Child in a married-couple subfamily"), R("5", "Child in a mother-child subfamily"),
                R("6", "Child in a father-child subfamily"))));
    colInfo.put("SMOCP", new Triple<>("selected_owner_cost_monthly", ColumnType.LONG, longSpecial(//
        R("bbbbb", -1L))));
    colInfo.put("SMP", new Triple<>("second_junior_mortgage_payment_monthly", ColumnType.LONG, longSpecial(//
        R("bbbbb", -1L))));
    colInfo.put("SMX",
        new Triple<>("second_junior_mortgage_status", ColumnType.STRING, new ReplaceFn(//
            R("b", "N/A (GQ/vacant/not owned or being bought)"), R("1", "Yes, a second mortgage"),
            R("2", "Yes, a home equity loan"), R("3", "No"), R("4", "Both a second mortgage and a home equity loan"))));
    colInfo
        .put("SOCP",
            new Triple<>("soc_occupation", ColumnType.STRING,
                new ReplaceFn(//
                    R("bbbbbb",
                        "N/A (less than 16 years old/NILF who last worked more than 5 years ago or never worked)"),
                    R("111021", "MGR-GENERAL AND OPERATIONS MANAGERS"),
                    R("1110XX", "MGR-CHIEF EXECUTIVES AND LEGISLATORS"),
                    R("112011", "MGR-ADVERTISING AND PROMOTIONS MANAGERS"),
                    R("112020", "MGR-MARKETING AND SALES MANAGERS"), R("112031", "MGR-PUBLIC RELATIONS MANAGERS"),
                    R("113011", "MGR-ADMINISTRATIVE SERVICES MANAGERS"), R("113021",
                        "MGR-COMPUTER AND INFORMATION SYSTEMS MANAGERS"),
                    R("113031", "MGR-FINANCIAL MANAGERS"), R("113040", "MGR-HUMAN RESOURCES MANAGERS"),
                    R("113051", "MGR-INDUSTRIAL PRODUCTION MANAGERS"), R("113061", "MGR-PURCHASING MANAGERS"),
                    R("113071", "MGR-TRANSPORTATION, STORAGE, AND DISTRIBUTION MANAGERS"),
                    R("119011", "MGR-FARM, RANCH, AND OTHER AGRICULTURAL MANAGERS"),
                    R("119012", "MGR-FARMERS AND RANCHERS"), R("119021", "MGR-CONSTRUCTION MANAGERS"),
                    R("119030", "MGR-EDUCATION ADMINISTRATORS"), R("119041", "MGR-ENGINEERING MANAGERS"),
                    R("119051", "MGR-FOOD SERVICE MANAGERS"), R("119061", "MGR-FUNERAL DIRECTORS"),
                    R("119071", "MGR-GAMING MANAGERS"), R("119081", "MGR-LODGING MANAGERS"),
                    R("119111", "MGR-MEDICAL AND HEALTH SERVICES MANAGERS"),
                    R("119121", "MGR-NATURAL SCIENCES MANAGERS"),
                    R("119141", "MGR-PROPERTY, REAL ESTATE, AND COMMUNITY ASSOCIATION MANAGERS"),
                    R("119151", "MGR-SOCIAL AND COMMUNITY SERVICE MANAGERS"),
                    R("1191XX", "MGR-MISCELLANEOUS MANAGERS, INCLUDING POSTMASTERS AND MAIL SUPERINTENDENTS"),
                    R("131011", "BUS-AGENTS AND BUSINESS MANAGERS OF ARTISTS, PERFORMERS, AND ATHLETES"),
                    R("131021", "BUS-PURCHASING AGENTS AND BUYERS, FARM PRODUCTS"),
                    R("131022", "BUS-WHOLESALE AND RETAIL BUYERS, EXCEPT FARM PRODUCTS"),
                    R("131023", "BUS-PURCHASING AGENTS, EXCEPT WHOLESALE, RETAIL, AND FARM PRODUCTS"),
                    R("131030", "BUS-CLAIMS ADJUSTERS, APPRAISERS, EXAMINERS, AND INVESTIGATORS"),
                    R("131041",
                        "BUS-COMPLIANCE OFFICERS, EXCEPT AGRICULTURE, CONSTRUCTION, HEALTH AND SAFETY, AND TRANSPORTATION"),
        R("131051", "BUS-COST ESTIMATORS"),
        R("131070", "BUS-HUMAN RESOURCES, TRAINING, AND LABOR RELATIONS SPECIALISTS"), R("131081", "BUS-LOGISTICIANS"),
        R("131111", "BUS-MANAGEMENT ANALYSTS"), R("131121", "BUS-MEETING AND CONVENTION PLANNERS"),
        R("131XXX", "BUS-OTHER BUSINESS OPERATIONS SPECIALISTS"), R("132011", "FIN-ACCOUNTANTS AND AUDITORS"),
        R("132021", "FIN-APPRAISERS AND ASSESSORS OF REAL ESTATE"), R("132031", "FIN-BUDGET ANALYSTS"),
        R("132041", "FIN-CREDIT ANALYSTS"), R("132051", "FIN-FINANCIAL ANALYSTS"),
        R("132052", "FIN-PERSONAL FINANCIAL ADVISORS"), R("132053", "FIN-INSURANCE UNDERWRITERS"),
        R("132061", "FIN-FINANCIAL EXAMINERS"), R("132070", "FIN-LOAN COUNSELORS AND OFFICERS"),
        R("132081", "FIN-TAX EXAMINERS, COLLECTORS, AND REVENUE AGENTS"), R("132082", "FIN-TAX PREPARERS"),
        R("132099", "FIN-FINANCIAL SPECIALISTS, ALL OTHER"), R("151021", "CMM-COMPUTER PROGRAMMERS"),
        R("151030", "CMM-COMPUTER SOFTWARE ENGINEERS"), R("151041", "CMM-COMPUTER SUPPORT SPECIALISTS"),
        R("151061", "CMM-DATABASE ADMINISTRATORS"), R("151071", "CMM-NETWORK AND COMPUTER SYSTEMS ADMINISTRATORS"),
        R("151081", "CMM-NETWORK SYSTEMS AND DATA COMMUNICATIONS ANALYSTS"),
        R("1510XX", "CMM-COMPUTER SCIENTISTS AND SYSTEMS ANALYSTS "), R("152011", "CMM-ACTUARIES"),
        R("152031", "CMM-OPERATIONS RESEARCH ANALYSTS"),
        R("1520XX", "CMM-MISCELLANEOUS MATHEMATICAL SCIENCE OCCUPATIONS, INCLUDING MATHEMATICIANS AND STATISTICIANS"),
        R("171010", "ENG-ARCHITECTS, EXCEPT NAVAL"), R("171020", "ENG-SURVEYORS, CARTOGRAPHERS, AND PHOTOGRAMMETRISTS"),
        R("172011", "ENG-AEROSPACE ENGINEERS"), R("172041", "ENG-CHEMICAL ENGINEERS"),
        R("172051", "ENG-CIVIL ENGINEERS"), R("172061", "ENG-COMPUTER HARDWARE ENGINEERS"),
        R("172070", "ENG-ELECTRICAL AND ELECTRONICS ENGINEERS"), R("172081", "ENG-ENVIRONMENTAL ENGINEERS"),
        R("1720XX", "ENG-BIOMEDICAL AND AGRICULTURAL ENGINEERS "),
        R("172110", "ENG-INDUSTRIAL ENGINEERS, INCLUDING HEALTH AND SAFETY"),
        R("172121", "ENG-MARINE ENGINEERS AND NAVAL ARCHITECTS"), R("172131", "ENG-MATERIALS ENGINEERS"),
        R("172141", "ENG-MECHANICAL ENGINEERS"),
        R("1721XX", "ENG-PETROLEUM, MINING AND GEOLOGICAL ENGINEERS, INCLUDING MINING SAFETY ENGINEERS "),
        R("1721YY", "ENG-MISCELLANEOUS ENGINEERS, INCLUDING NUCLEAR ENGINEERS "), R("173010", "ENG-DRAFTERS"),
        R("173020", "ENG-ENGINEERING TECHNICIANS, EXCEPT DRAFTERS"),
        R("173031", "ENG-SURVEYING AND MAPPING TECHNICIANS"), R("191010", "SCI-AGRICULTURAL AND FOOD SCIENTISTS"),
        R("191020", "SCI-BIOLOGICAL SCIENTISTS"), R("191030", "SCI-CONSERVATION SCIENTISTS AND FORESTERS"),
        R("191040", "SCI-MEDICAL SCIENTISTS"), R("192010", "SCI-ASTRONOMERS AND PHYSICISTS"),
        R("192021", "SCI-ATMOSPHERIC AND SPACE SCIENTISTS"), R("192030", "SCI-CHEMISTS AND MATERIALS SCIENTISTS"),
        R("192040", "SCI-ENVIRONMENTAL SCIENTISTS AND GEOSCIENTISTS"),
        R("192099", "SCI-PHYSICAL SCIENTISTS, ALL OTHER"), R("193011", "SCI-ECONOMISTS"),
        R("193020", "SCI-MARKET AND SURVEY RESEARCHERS"), R("193030", "SCI-PSYCHOLOGISTS"),
        R("193051", "SCI-URBAN AND REGIONAL PLANNERS"),
        R("1930XX", "SCI-MISCELLANEOUS SOCIAL SCIENTISTS, INCLUDING SOCIOLOGISTS"),
        R("194011", "SCI-AGRICULTURAL AND FOOD SCIENCE TECHNICIANS"), R("194021", "SCI-BIOLOGICAL TECHNICIANS"),
        R("194031", "SCI-CHEMICAL TECHNICIANS"), R("194041", "SCI-GEOLOGICAL AND PETROLEUM TECHNICIANS"),
        R("1940XX",
            "SCI-MISCELLANEOUS LIFE, PHYSICAL, AND SOCIAL SCIENCE TECHNICIANS, INCLUDING SOCIAL SCIENCE RESEARCH ASSISTANTS AND NUCLEAR TECHNICIANS"),
        R("211010", "CMS-COUNSELORS"), R("211020", "CMS-SOCIAL WORKERS"),
        R("211090", "CMS-MISCELLANEOUS COMMUNITY AND SOCIAL SERVICE SPECIALISTS"), R("212011", "CMS-CLERGY"),
        R("212021", "CMS-DIRECTORS, RELIGIOUS ACTIVITIES AND EDUCATION"),
        R("212099", "CMS-RELIGIOUS WORKERS, ALL OTHER"),
        R("2310XX", "LGL-LAWYERS, AND JUDGES, MAGISTRATES, AND OTHER JUDICIAL WORKERS"),
        R("232011", "LGL-PARALEGALS AND LEGAL ASSISTANTS"), R("232090", "LGL-MISCELLANEOUS LEGAL SUPPORT WORKERS"),
        R("251000", "EDU-POSTSECONDARY TEACHERS"), R("252010", "EDU-PRESCHOOL AND KINDERGARTEN TEACHERS"),
        R("252020", "EDU-ELEMENTARY AND MIDDLE SCHOOL TEACHERS"), R("252030", "EDU-SECONDARY SCHOOL TEACHERS"),
        R("252040", "EDU-SPECIAL EDUCATION TEACHERS"), R("253000", "EDU-OTHER TEACHERS AND INSTRUCTORS"),
        R("254010", "EDU-ARCHIVISTS, CURATORS, AND MUSEUM TECHNICIANS"), R("254021", "EDU-LIBRARIANS"),
        R("254031", "EDU-LIBRARY TECHNICIANS"), R("259041", "EDU-TEACHER ASSISTANTS"),
        R("2590XX", "EDU-OTHER EDUCATION, TRAINING, AND LIBRARY WORKERS"),
        R("271010", "ENT-ARTISTS AND RELATED WORKERS"), R("271020", "ENT-DESIGNERS"), R("272011", "ENT-ACTORS"),
        R("272012", "ENT-PRODUCERS AND DIRECTORS"), R("272020", "ENT-ATHLETES, COACHES, UMPIRES, AND RELATED WORKERS"),
        R("272030", "ENT-DANCERS AND CHOREOGRAPHERS"), R("272040", "ENT-MUSICIANS, SINGERS, AND RELATED WORKERS"),
        R("272099", "ENT-ENTERTAINERS AND PERFORMERS, SPORTS AND RELATED WORKERS, ALL OTHER"),
        R("273010", "ENT-ANNOUNCERS"), R("273020", "ENT-NEWS ANALYSTS, REPORTERS AND CORRESPONDENTS"),
        R("273031", "ENT-PUBLIC RELATIONS SPECIALISTS"), R("273041", "ENT-EDITORS"),
        R("273042", "ENT-TECHNICAL WRITERS"), R("273043", "ENT-WRITERS AND AUTHORS"),
        R("273090", "ENT-MISCELLANEOUS MEDIA AND COMMUNICATION WORKERS"), R("274021", "ENT-PHOTOGRAPHERS"),
        R("274030", "ENT-TELEVISION, VIDEO, AND MOTION PICTURE CAMERA OPERATORS AND EDITORS"),
        R("2740XX",
            "ENT-BROADCAST AND SOUND ENGINEERING TECHNICIANS AND RADIO OPERATORS, AND MEDIA AND COMMUNICATION EQUIPMENT WORKERS, ALL OTHER "),
        R("291011", "MED-CHIROPRACTORS"), R("291020", "MED-DENTISTS"), R("291031", "MED-DIETITIANS AND NUTRITIONISTS"),
        R("291041", "MED-OPTOMETRISTS"), R("291051", "MED-PHARMACISTS"), R("291060", "MED-PHYSICIANS AND SURGEONS"),
        R("291071", "MED-PHYSICIAN ASSISTANTS"), R("291081", "MED-PODIATRISTS"), R("291111", "MED-REGISTERED NURSES"),
        R("291121", "MED-AUDIOLOGISTS"), R("291122", "MED-OCCUPATIONAL THERAPISTS"),
        R("291123", "MED-PHYSICAL THERAPISTS"), R("291124", "MED-RADIATION THERAPISTS"),
        R("291125", "MED-RECREATIONAL THERAPISTS"), R("291126", "MED-RESPIRATORY THERAPISTS"),
        R("291127", "MED-SPEECH-LANGUAGE PATHOLOGISTS"), R("291129", "MED-THERAPISTS, ALL OTHER"),
        R("291131", "MED-VETERINARIANS"), R("291199", "MED-HEALTH DIAGNOSING AND TREATING PRACTITIONERS, ALL OTHER"),
        R("292010", "MED-CLINICAL LABORATORY TECHNOLOGISTS AND TECHNICIANS"), R("292021", "MED-DENTAL HYGIENISTS"),
        R("292030", "MED-DIAGNOSTIC RELATED TECHNOLOGISTS AND TECHNICIANS"),
        R("292041", "MED-EMERGENCY MEDICAL TECHNICIANS AND PARAMEDICS"),
        R("292050", "MED-HEALTH DIAGNOSING AND TREATING PRACTITIONER SUPPORT TECHNICIANS"),
        R("292061", "MED-LICENSED PRACTICAL AND LICENSED VOCATIONAL NURSES"),
        R("292071", "MED-MEDICAL RECORDS AND HEALTH INFORMATION TECHNICIANS"), R("292081", "MED-OPTICIANS, DISPENSING"),
        R("292090", "MED-MISCELLANEOUS HEALTH TECHNOLOGISTS AND TECHNICIANS"),
        R("299000", "MED-OTHER HEALTHCARE PRACTITIONERS AND TECHNICAL OCCUPATIONS"),
        R("311010", "HLS-NURSING, PSYCHIATRIC, AND HOME HEALTH AIDES"),
        R("312010", "HLS-OCCUPATIONAL THERAPIST ASSISTANTS AND AIDES"),
        R("312020", "HLS-PHYSICAL THERAPIST ASSISTANTS AND AIDES"), R("319011", "HLS-MASSAGE THERAPISTS"),
        R("319091", "HLS-DENTAL ASSISTANTS"),
        R("31909X", "HLS-MEDICAL ASSISTANTS AND OTHER HEALTHCARE SUPPORT OCCUPATIONS, EXCEPT DENTAL ASSISTANTS"),
        R("331011", "PRT-FIRST-LINE SUPERVISORS/MANAGERS OF CORRECTIONAL OFFICERS"),
        R("331012", "PRT-FIRST-LINE SUPERVISORS/MANAGERS OF POLICE AND DETECTIVES"),
        R("331021", "PRT-FIRST-LINE SUPERVISORS/MANAGERS OF FIRE FIGHTING AND PREVENTION WORKERS"),
        R("331099", "PRT-SUPERVISORS, PROTECTIVE SERVICE WORKERS, ALL OTHER"), R("332011", "PRT-FIRE FIGHTERS"),
        R("332020", "PRT-FIRE INSPECTORS"), R("333010", "PRT-BAILIFFS, CORRECTIONAL OFFICERS, AND JAILERS"),
        R("333021", "PRT-DETECTIVES AND CRIMINAL INVESTIGATORS"), R("333050", "PRT-POLICE OFFICERS"),
        R("3330XX", "PRT-MISCELLANEOUS LAW ENFORCEMENT WORKERS"), R("339011", "PRT-ANIMAL CONTROL WORKERS"),
        R("339021", "PRT-PRIVATE DETECTIVES AND INVESTIGATORS"),
        R("339030", "PRT-SECURITY GUARDS AND GAMING SURVEILLANCE OFFICERS"), R("339091", "PRT-CROSSING GUARDS"),
        R("33909X", "PRT-LIFEGUARDS AND OTHER PROTECTIVE SERVICE WORKERS"), R("351011", "EAT-CHEFS AND HEAD COOKS"),
        R("351012", "EAT-FIRST-LINE SUPERVISORS/MANAGERS OF FOOD PREPARATION AND SERVING WORKERS"),
        R("352010", "EAT-COOKS"), R("352021", "EAT-FOOD PREPARATION WORKERS"), R("353011", "EAT-BARTENDERS"),
        R("353021", "EAT-COMBINED FOOD PREPARATION AND SERVING WORKERS, INCLUDING FAST FOOD"),
        R("353022", "EAT-COUNTER ATTENDANTS, CAFETERIA, FOOD CONCESSION, AND COFFEE SHOP"),
        R("353031", "EAT-WAITERS AND WAITRESSES"), R("353041", "EAT-FOOD SERVERS, NONRESTAURANT"),
        R("359021", "EAT-DISHWASHERS"), R("359031", "EAT-HOSTS AND HOSTESSES, RESTAURANT, LOUNGE, AND COFFEE SHOP"),
        R("3590XX",
            "EAT-MISCELLANEOUS FOOD PREPARATION AND SERVING RELATED WORKERS, INCLUDING DINING ROOM AND CAFETERIA ATTENDANTS AND BARTENDER HELPERS"),
        R("371011", "CLN-FIRST-LINE SUPERVISORS/MANAGERS OF HOUSEKEEPING AND JANITORIAL WORKERS"),
        R("371012", "CLN-FIRST-LINE SUPERVISORS/MANAGERS OF LANDSCAPING, LAWN SERVICE, AND GROUNDSKEEPING WORKERS"),
        R("372012", "CLN-MAIDS AND HOUSEKEEPING CLEANERS"), R("37201X", "CLN-JANITORS AND BUILDING CLEANERS"),
        R("372021", "CLN-PEST CONTROL WORKERS"), R("373010", "CLN-GROUNDS MAINTENANCE WORKERS"),
        R("391010", "PRS-FIRST-LINE SUPERVISORS/MANAGERS OF GAMING WORKERS"),
        R("391021", "PRS-FIRST-LINE SUPERVISORS/MANAGERS OF PERSONAL SERVICE WORKERS"),
        R("392011", "PRS-ANIMAL TRAINERS"), R("392021", "PRS-NONFARM ANIMAL CARETAKERS"),
        R("393010", "PRS-GAMING SERVICES WORKERS"), R("393021", "PRS-MOTION PICTURE PROJECTIONISTS"),
        R("393031", "PRS-USHERS, LOBBY ATTENDANTS, AND TICKET TAKERS"),
        R("393090", "PRS-MISCELLANEOUS ENTERTAINMENT ATTENDANTS AND RELATED WORKERS"),
        R("394000", "PRS-FUNERAL SERVICE WORKERS"), R("395011", "PRS-BARBERS"),
        R("395012", "PRS-HAIRDRESSERS, HAIRSTYLISTS, AND COSMETOLOGISTS"),
        R("395090", "PRS-MISCELLANEOUS PERSONAL APPEARANCE WORKERS"),
        R("396010", "PRS-BAGGAGE PORTERS, BELLHOPS, AND CONCIERGES"), R("396020", "PRS-TOUR AND TRAVEL GUIDES"),
        R("396030", "PRS-TRANSPORTATION ATTENDANTS"), R("399011", "PRS-CHILD CARE WORKERS"),
        R("399021", "PRS-PERSONAL AND HOME CARE AIDES"), R("399030", "PRS-RECREATION AND FITNESS WORKERS"),
        R("399041", "PRS-RESIDENTIAL ADVISORS"), R("399099", "PRS-PERSONAL CARE AND SERVICE WORKERS, ALL OTHER"),
        R("411011", "SAL-FIRST-LINE SUPERVISORS/MANAGERS OF RETAIL SALES WORKERS"),
        R("411012", "SAL-FIRST-LINE SUPERVISORS/MANAGERS OF NON-RETAIL SALES WORKERS"), R("412010", "SAL-CASHIERS"),
        R("412021", "SAL-COUNTER AND RENTAL CLERKS"), R("412022", "SAL-PARTS SALESPERSONS"),
        R("412031", "SAL-RETAIL SALESPERSONS"), R("413011", "SAL-ADVERTISING SALES AGENTS"),
        R("413021", "SAL-INSURANCE SALES AGENTS"),
        R("413031", "SAL-SECURITIES, COMMODITIES, AND FINANCIAL SERVICES SALES AGENTS"),
        R("413041", "SAL-TRAVEL AGENTS"), R("413099", "SAL-SALES REPRESENTATIVES, SERVICES, ALL OTHER"),
        R("414010", "SAL-SALES REPRESENTATIVES, WHOLESALE AND MANUFACTURING"),
        R("419010", "SAL-MODELS, DEMONSTRATORS, AND PRODUCT PROMOTERS"),
        R("419020", "SAL-REAL ESTATE BROKERS AND SALES AGENTS"), R("419031", "SAL-SALES ENGINEERS"),
        R("419041", "SAL-TELEMARKETERS"),
        R("419091", "SAL-DOOR-TO-DOOR SALES WORKERS, NEWS AND STREET VENDORS, AND RELATED WORKERS"),
        R("419099", "SAL-SALES AND RELATED WORKERS, ALL OTHER"),
        R("431011", "OFF-FIRST-LINE SUPERVISORS/MANAGERS OF OFFICE AND ADMINISTRATIVE SUPPORT WORKERS"),
        R("432011", "OFF-SWITCHBOARD OPERATORS, INCLUDING ANSWERING SERVICE"), R("432021", "OFF-TELEPHONE OPERATORS"),
        R("432099", "OFF-COMMUNICATIONS EQUIPMENT OPERATORS, ALL OTHER"),
        R("433011", "OFF-BILL AND ACCOUNT COLLECTORS"),
        R("433021", "OFF-BILLING AND POSTING CLERKS AND MACHINE OPERATORS"),
        R("433031", "OFF-BOOKKEEPING, ACCOUNTING, AND AUDITING CLERKS"), R("433041", "OFF-GAMING CAGE WORKERS"),
        R("433051", "OFF-PAYROLL AND TIMEKEEPING CLERKS"), R("433061", "OFF-PROCUREMENT CLERKS"),
        R("433071", "OFF-TELLERS"), R("434011", "OFF-BROKERAGE CLERKS"),
        R("434031", "OFF-COURT, MUNICIPAL, AND LICENSE CLERKS"),
        R("434041", "OFF-CREDIT AUTHORIZERS, CHECKERS, AND CLERKS"),
        R("434051", "OFF-CUSTOMER SERVICE REPRESENTATIVES"),
        R("434061", "OFF-ELIGIBILITY INTERVIEWERS, GOVERNMENT PROGRAMS"), R("434071", "OFF-FILE CLERKS"),
        R("434081", "OFF-HOTEL, MOTEL, AND RESORT DESK CLERKS"),
        R("434111", "OFF-INTERVIEWERS, EXCEPT ELIGIBILITY AND LOAN"), R("434121", "OFF-LIBRARY ASSISTANTS, CLERICAL"),
        R("434131", "OFF-LOAN INTERVIEWERS AND CLERKS"), R("434141", "OFF-NEW ACCOUNTS CLERKS"),
        R("434161", "OFF-HUMAN RESOURCES ASSISTANTS, EXCEPT PAYROLL AND TIMEKEEPING"),
        R("434171", "OFF-RECEPTIONISTS AND INFORMATION CLERKS"),
        R("434181", "OFF-RESERVATION AND TRANSPORTATION TICKET AGENTS AND TRAVEL CLERKS"),
        R("434199", "OFF-INFORMATION AND RECORD CLERKS, ALL OTHER"),
        R("434XXX", "OFF-CORRESPONDENCE CLERKS AND ORDER CLERKS "), R("435011", "OFF-CARGO AND FREIGHT AGENTS"),
        R("435021", "OFF-COURIERS AND MESSENGERS"), R("435030", "OFF-DISPATCHERS"),
        R("435041", "OFF-METER READERS, UTILITIES"), R("435051", "OFF-POSTAL SERVICE CLERKS"),
        R("435052", "OFF-POSTAL SERVICE MAIL CARRIERS"),
        R("435053", "OFF-POSTAL SERVICE MAIL SORTERS, PROCESSORS, AND PROCESSING MACHINE OPERATORS"),
        R("435061", "OFF-PRODUCTION, PLANNING, AND EXPEDITING CLERKS"),
        R("435071", "OFF-SHIPPING, RECEIVING, AND TRAFFIC CLERKS"), R("435081", "OFF-STOCK CLERKS AND ORDER FILLERS"),
        R("435111", "OFF-WEIGHERS, MEASURERS, CHECKERS, AND SAMPLERS, RECORDKEEPING"),
        R("436010", "OFF-SECRETARIES AND ADMINISTRATIVE ASSISTANTS"), R("439011", "OFF-COMPUTER OPERATORS"),
        R("439021", "OFF-DATA ENTRY KEYERS"), R("439022", "OFF-WORD PROCESSORS AND TYPISTS"),
        R("439041", "OFF-INSURANCE CLAIMS AND POLICY PROCESSING CLERKS"),
        R("439051", "OFF-MAIL CLERKS AND MAIL MACHINE OPERATORS, EXCEPT POSTAL SERVICE"),
        R("439061", "OFF-OFFICE CLERKS, GENERAL"), R("439071", "OFF-OFFICE MACHINE OPERATORS, EXCEPT COMPUTER"),
        R("439081", "OFF-PROOFREADERS AND COPY MARKERS"), R("439111", "OFF-STATISTICAL ASSISTANTS"),
        R("439XXX", "OFF-MISCELLANEOUS OFFICE AND ADMINISTRATIVE SUPPORT WORKERS, INCLUDING DESKTOP PUBLISHERS "),
        R("451010", "FFF-FIRST-LINE SUPERVISORS/MANAGERS OF FARMING, FISHING, AND FORESTRY WORKERS"),
        R("452011", "FFF-AGRICULTURAL INSPECTORS"), R("452041", "FFF-GRADERS AND SORTERS, AGRICULTURAL PRODUCTS"),
        R("4520XX", "FFF-MISCELLANEOUS AGRICULTURAL WORKERS, INCLUDING ANIMAL BREEDERS "),
        R("453000", "FFF-FISHING AND HUNTING WORKERS"), R("454011", "FFF-FOREST AND CONSERVATION WORKERS"),
        R("454020", "FFF-LOGGING WORKERS"),
        R("471011", "CON-FIRST-LINE SUPERVISORS/MANAGERS OF CONSTRUCTION TRADES AND EXTRACTION WORKERS"),
        R("472011", "CON-BOILERMAKERS"), R("472020", "CON-BRICKMASONS, BLOCKMASONS, AND STONEMASONS"),
        R("472031", "CON-CARPENTERS"), R("472040", "CON-CARPET, FLOOR, AND TILE INSTALLERS AND FINISHERS"),
        R("472050", "CON-CEMENT MASONS, CONCRETE FINISHERS, AND TERRAZZO WORKERS"),
        R("472061", "CON-CONSTRUCTION LABORERS"), R("472071", "CON-PAVING, SURFACING, AND TAMPING EQUIPMENT OPERATORS"),
        R("47207X", "CON-CONSTRUCTION EQUIPMENT OPERATORS, EXCEPT PAVING, SURFACING, AND TAMPING EQUIPMENT OPERATORS "),
        R("472080", "CON-DRYWALL INSTALLERS, CEILING TILE INSTALLERS, AND TAPERS"), R("472111", "CON-ELECTRICIANS"),
        R("472121", "CON-GLAZIERS"), R("472130", "CON-INSULATION WORKERS"),
        R("472141", "CON-PAINTERS, CONSTRUCTION AND MAINTENANCE"), R("472142", "CON-PAPERHANGERS"),
        R("472150", "CON-PIPELAYERS, PLUMBERS, PIPEFITTERS, AND STEAMFITTERS"),
        R("472161", "CON-PLASTERERS AND STUCCO MASONS"), R("472171", "CON-REINFORCING IRON AND REBAR WORKERS"),
        R("472181", "CON-ROOFERS"), R("472211", "CON-SHEET METAL WORKERS"),
        R("472221", "CON-STRUCTURAL IRON AND STEEL WORKERS"), R("473010", "CON-HELPERS, CONSTRUCTION TRADES"),
        R("474011", "CON-CONSTRUCTION AND BUILDING INSPECTORS"), R("474021", "CON-ELEVATOR INSTALLERS AND REPAIRERS"),
        R("474031", "CON-FENCE ERECTORS"), R("474041", "CON-HAZARDOUS MATERIALS REMOVAL WORKERS"),
        R("474051", "CON-HIGHWAY MAINTENANCE WORKERS"),
        R("474061", "CON-RAIL-TRACK LAYING AND MAINTENANCE EQUIPMENT OPERATORS"),
        R("4740XX", "CON-MISCELLANEOUS CONSTRUCTION WORKERS, INCLUDING SEPTIC TANK SERVICERS AND SEWER PIPE CLEANERS "),
        R("475021", "EXT-EARTH DRILLERS, EXCEPT OIL AND GAS"),
        R("475031", "EXT-EXPLOSIVES WORKERS, ORDNANCE HANDLING EXPERTS, AND BLASTERS"),
        R("475040", "EXT-MINING MACHINE OPERATORS"),
        R("4750XX", "EXT-MISCELLANEOUS EXTRACTION WORKERS, INCLUDING ROOF BOLTERS AND HELPERS "),
        R("4750YY", "EXT-DERRICK, ROTARY DRILL, AND SERVICE UNIT OPERATORS, AND ROUSTABOUTS, OIL, GAS, AND MINING "),
        R("491011", "RPR-FIRST-LINE SUPERVISORS/MANAGERS OF MECHANICS, INSTALLERS, AND REPAIRERS"),
        R("492011", "RPR-COMPUTER, AUTOMATED TELLER, AND OFFICE MACHINE REPAIRERS"),
        R("492020", "RPR-RADIO AND TELECOMMUNICATIONS EQUIPMENT INSTALLERS AND REPAIRERS"),
        R("492091", "RPR-AVIONICS TECHNICIANS"), R("492092", "RPR-ELECTRIC MOTOR, POWER TOOL, AND RELATED REPAIRERS"),
        R("492096", "RPR-ELECTRONIC EQUIPMENT INSTALLERS AND REPAIRERS, MOTOR VEHICLES"),
        R("492097", "RPR-ELECTRONIC HOME ENTERTAINMENT EQUIPMENT INSTALLERS AND REPAIRERS"),
        R("492098", "RPR-SECURITY AND FIRE ALARM SYSTEMS INSTALLERS"),
        R("49209X", "RPR-ELECTRICAL AND ELECTRONICS REPAIRERS, TRANSPORTATION EQUIPMENT, AND INDUSTRIAL AND UTILITY "),
        R("493011", "RPR-AIRCRAFT MECHANICS AND SERVICE TECHNICIANS"),
        R("493021", "RPR-AUTOMOTIVE BODY AND RELATED REPAIRERS"),
        R("493022", "RPR-AUTOMOTIVE GLASS INSTALLERS AND REPAIRERS"),
        R("493023", "RPR-AUTOMOTIVE SERVICE TECHNICIANS AND MECHANICS"),
        R("493031", "RPR-BUS AND TRUCK MECHANICS AND DIESEL ENGINE SPECIALISTS"),
        R("493040", "RPR-HEAVY VEHICLE AND MOBILE EQUIPMENT SERVICE TECHNICIANS AND MECHANICS"),
        R("493050", "RPR-SMALL ENGINE MECHANICS"),
        R("493090", "RPR-MISCELLANEOUS VEHICLE AND MOBILE EQUIPMENT MECHANICS, INSTALLERS, AND REPAIRERS"),
        R("499010", "RPR-CONTROL AND VALVE INSTALLERS AND REPAIRERS"),
        R("499021", "RPR-HEATING, AIR CONDITIONING, AND REFRIGERATION MECHANICS AND INSTALLERS"),
        R("499031", "RPR-HOME APPLIANCE REPAIRERS"), R("499042", "RPR-MAINTENANCE AND REPAIR WORKERS, GENERAL"),
        R("499043", "RPR-MAINTENANCE WORKERS, MACHINERY"), R("499044", "RPR-MILLWRIGHTS"),
        R("49904X", "RPR-INDUSTRIAL AND REFRACTORY MACHINERY MECHANICS "),
        R("499051", "RPR-ELECTRICAL POWER-LINE INSTALLERS AND REPAIRERS"),
        R("499052", "RPR-TELECOMMUNICATIONS LINE INSTALLERS AND REPAIRERS"),
        R("499060", "RPR-PRECISION INSTRUMENT AND EQUIPMENT REPAIRERS"),
        R("499091", "RPR-COIN, VENDING, AND AMUSEMENT MACHINE SERVICERS AND REPAIRERS"),
        R("499094", "RPR-LOCKSMITHS AND SAFE REPAIRERS"),
        R("499095", "RPR-MANUFACTURED BUILDING AND MOBILE HOME INSTALLERS"), R("499096", "RPR-RIGGERS"),
        R("499098", "RPR-HELPERS--INSTALLATION, MAINTENANCE, AND REPAIR WORKERS"),
        R("49909X",
            "RPR-OTHER INSTALLATION, MAINTENANCE, AND REPAIR WORKERS, INCLUDING COMMERCIAL DIVERS, AND SIGNAL AND TRACK SWITCH REPAIRERS "),
        R("511011", "PRD-FIRST-LINE SUPERVISORS/MANAGERS OF PRODUCTION AND OPERATING WORKERS"),
        R("512011", "PRD-AIRCRAFT STRUCTURE, SURFACES, RIGGING, AND SYSTEMS ASSEMBLERS"),
        R("512020", "PRD-ELECTRICAL, ELECTRONICS, AND ELECTROMECHANICAL ASSEMBLERS"),
        R("512031", "PRD-ENGINE AND OTHER MACHINE ASSEMBLERS"),
        R("512041", "PRD-STRUCTURAL METAL FABRICATORS AND FITTERS"),
        R("512090", "PRD-MISCELLANEOUS ASSEMBLERS AND FABRICATORS"), R("513011", "PRD-BAKERS"),
        R("513020", "PRD-BUTCHERS AND OTHER MEAT, POULTRY, AND FISH PROCESSING WORKERS"),
        R("513091", "PRD-FOOD AND TOBACCO ROASTING, BAKING, AND DRYING MACHINE OPERATORS AND TENDERS"),
        R("513092", "PRD-FOOD BATCHMAKERS"), R("513093", "PRD-FOOD COOKING MACHINE OPERATORS AND TENDERS"),
        R("514010", "PRD-COMPUTER CONTROL PROGRAMMERS AND OPERATORS"),
        R("514021", "PRD-EXTRUDING AND DRAWING MACHINE SETTERS, OPERATORS, AND TENDERS, METAL AND PLASTIC"),
        R("514022", "PRD-FORGING MACHINE SETTERS, OPERATORS, AND TENDERS, METAL AND PLASTIC"),
        R("514023", "PRD-ROLLING MACHINE SETTERS, OPERATORS, AND TENDERS, METAL AND PLASTIC"),
        R("514031", "PRD-CUTTING, PUNCHING, AND PRESS MACHINE SETTERS, OPERATORS, AND TENDERS, METAL AND PLASTIC"),
        R("514032", "PRD-DRILLING AND BORING MACHINE TOOL SETTERS, OPERATORS, AND TENDERS, METAL AND PLASTIC"),
        R("514033",
            "PRD-GRINDING, LAPPING, POLISHING, AND BUFFING MACHINE TOOL SETTERS, OPERATORS, AND TENDERS, METAL AND PLASTIC"),
        R("514034", "PRD-LATHE AND TURNING MACHINE TOOL SETTERS, OPERATORS, AND TENDERS, METAL AND PLASTIC"),
        R("514041", "PRD-MACHINISTS"), R("514050", "PRD-METAL FURNACE AND KILN OPERATORS AND TENDERS"),
        R("514060", "PRD-MODEL MAKERS AND PATTERNMAKERS, METAL AND PLASTIC"),
        R("514070", "PRD-MOLDERS AND MOLDING MACHINE SETTERS, OPERATORS, AND TENDERS, METAL AND PLASTIC"),
        R("514111", "PRD-TOOL AND DIE MAKERS"), R("514120", "PRD-WELDING, SOLDERING, AND BRAZING WORKERS"),
        R("514191", "PRD-HEAT TREATING EQUIPMENT SETTERS, OPERATORS, AND TENDERS, METAL AND PLASTIC"),
        R("514193", "PRD-PLATING AND COATING MACHINE SETTERS, OPERATORS, AND TENDERS, METAL AND PLASTIC"),
        R("514194", "PRD-TOOL GRINDERS, FILERS, AND SHARPENERS"),
        R("514XXX",
            "PRD-MISCELLANEOUS METAL WORKERS AND PLASTIC WORKERS, INCLUDING MILLING AND PLANING MACHINE SETTERS, AND MULTIPLE MACHINE TOOL SETTERS, AND LAY-OUT WORKERS"),
        R("515010", "PRD-BOOKBINDERS AND BINDERY WORKERS"), R("515021", "PRD-JOB PRINTERS"),
        R("515022", "PRD-PREPRESS TECHNICIANS AND WORKERS"), R("515023", "PRD-PRINTING MACHINE OPERATORS"),
        R("516011", "PRD-LAUNDRY AND DRY-CLEANING WORKERS"),
        R("516021", "PRD-PRESSERS, TEXTILE, GARMENT, AND RELATED MATERIALS"),
        R("516031", "PRD-SEWING MACHINE OPERATORS"), R("516041", "PRD-SHOE AND LEATHER WORKERS AND REPAIRERS"),
        R("516042", "PRD-SHOE MACHINE OPERATORS AND TENDERS"), R("516050", "PRD-TAILORS, DRESSMAKERS, AND SEWERS"),
        R("516063", "PRD-TEXTILE KNITTING AND WEAVING MACHINE SETTERS, OPERATORS, AND TENDERS"),
        R("516064", "PRD-TEXTILE WINDING, TWISTING, AND DRAWING OUT MACHINE SETTERS, OPERATORS, AND TENDERS"),
        R("51606X", "PRD-TEXTILE BLEACHING AND DYEING, AND CUTTING MACHINE SETTERS, OPERATORS, AND TENDERS "),
        R("516093", "PRD-UPHOLSTERERS"),
        R("51609X", "PRD-MISCELLANEOUS TEXTILE, APPAREL, AND FURNISHINGS WORKERS, EXCEPT UPHOLSTERERS "),
        R("517011", "PRD-CABINETMAKERS AND BENCH CARPENTERS"), R("517021", "PRD-FURNITURE FINISHERS"),
        R("517041", "PRD-SAWING MACHINE SETTERS, OPERATORS, AND TENDERS, WOOD"),
        R("517042", "PRD-WOODWORKING MACHINE SETTERS, OPERATORS, AND TENDERS, EXCEPT SAWING"),
        R("5170XX", "PRD-MISCELLANEOUS WOODWORKERS, INCLUDING MODEL MAKERS AND PATTERNMAKERS "),
        R("518010", "PRD-POWER PLANT OPERATORS, DISTRIBUTORS, AND DISPATCHERS"),
        R("518021", "PRD-STATIONARY ENGINEERS AND BOILER OPERATORS"),
        R("518031", "PRD-WATER AND LIQUID WASTE TREATMENT PLANT AND SYSTEM OPERATORS"),
        R("518090", "PRD-MISCELLANEOUS PLANT AND SYSTEM OPERATORS"),
        R("519010", "PRD-CHEMICAL PROCESSING MACHINE SETTERS, OPERATORS, AND TENDERS"),
        R("519020", "PRD-CRUSHING, GRINDING, POLISHING, MIXING, AND BLENDING WORKERS"),
        R("519030", "PRD-CUTTING WORKERS"),
        R("519041", "PRD-EXTRUDING, FORMING, PRESSING, AND COMPACTING MACHINE SETTERS, OPERATORS, AND TENDERS"),
        R("519051", "PRD-FURNACE, KILN, OVEN, DRIER, AND KETTLE OPERATORS AND TENDERS"),
        R("519061", "PRD-INSPECTORS, TESTERS, SORTERS, SAMPLERS, AND WEIGHERS"),
        R("519071", "PRD-JEWELERS AND PRECIOUS STONE AND METAL WORKERS"),
        R("519080", "PRD-MEDICAL, DENTAL, AND OPHTHALMIC LABORATORY TECHNICIANS"),
        R("519111", "PRD-PACKAGING AND FILLING MACHINE OPERATORS AND TENDERS"), R("519120", "PRD-PAINTING WORKERS"),
        R("519130", "PRD-PHOTOGRAPHIC PROCESS WORKERS AND PROCESSING MACHINE OPERATORS"),
        R("519191", "PRD-CEMENTING AND GLUING MACHINE OPERATORS AND TENDERS"),
        R("519192", "PRD-CLEANING, WASHING, AND METAL PICKLING EQUIPMENT OPERATORS AND TENDERS"),
        R("519194", "PRD-ETCHERS AND ENGRAVERS"),
        R("519195", "PRD-MOLDERS, SHAPERS, AND CASTERS, EXCEPT METAL AND PLASTIC"),
        R("519196", "PRD-PAPER GOODS MACHINE SETTERS, OPERATORS, AND TENDERS"), R("519197", "PRD-TIRE BUILDERS"),
        R("519198", "PRD-HELPERS-PRODUCTION WORKERS"),
        R("5191XX",
            "PRD-OTHER PRODUCTION WORKERS, INCLUDING SEMICONDUCTOR PROCESSORS AND COOLING AND FREEZING EQUIPMENT OPERATORS "),
        R("531000", "TRN-SUPERVISORS, TRANSPORTATION AND MATERIAL MOVING WORKERS"),
        R("532010", "TRN-AIRCRAFT PILOTS AND FLIGHT ENGINEERS"),
        R("532020", "TRN-AIR TRAFFIC CONTROLLERS AND AIRFIELD OPERATIONS SPECIALISTS"),
        R("533011", "TRN-AMBULANCE DRIVERS AND ATTENDANTS, EXCEPT EMERGENCY MEDICAL TECHNICIANS"),
        R("533020", "TRN-BUS DRIVERS"), R("533030", "TRN-DRIVER/SALES WORKERS AND TRUCK DRIVERS"),
        R("533041", "TRN-TAXI DRIVERS AND CHAUFFEURS"), R("533099", "TRN-MOTOR VEHICLE OPERATORS, ALL OTHER"),
        R("534010", "TRN-LOCOMOTIVE ENGINEERS AND OPERATORS"),
        R("534021", "TRN-RAILROAD BRAKE, SIGNAL, AND SWITCH OPERATORS"),
        R("534031", "TRN-RAILROAD CONDUCTORS AND YARDMASTERS"),
        R("5340XX", "TRN-SUBWAY, STREETCAR, AND OTHER RAIL TRANSPORTATION WORKERS "),
        R("535020", "TRN-SHIP AND BOAT CAPTAINS AND OPERATORS"),
        R("5350XX", "TRN-SAILORS AND MARINE OILERS, AND SHIP ENGINEERS "), R("536021", "TRN-PARKING LOT ATTENDANTS"),
        R("536031", "TRN-SERVICE STATION ATTENDANTS"), R("536051", "TRN-TRANSPORTATION INSPECTORS"),
        R("5360XX",
            "TRN-MISCELLANEOUS TRANSPORTATION WORKERS, INCLUDING BRIDGE AND LOCK TENDERS AND TRAFFIC TECHNICIANS "),
        R("537021", "TRN-CRANE AND TOWER OPERATORS"),
        R("537030", "TRN-DREDGE, EXCAVATING, AND LOADING MACHINE OPERATORS"),
        R("537051", "TRN-INDUSTRIAL TRUCK AND TRACTOR OPERATORS"),
        R("537061", "TRN-CLEANERS OF VEHICLES AND EQUIPMENT"),
        R("537062", "TRN-LABORERS AND FREIGHT, STOCK, AND MATERIAL MOVERS, HAND"),
        R("537063", "TRN-MACHINE FEEDERS AND OFFBEARERS"), R("537064", "TRN-PACKERS AND PACKAGERS, HAND"),
        R("537070", "TRN-PUMPING STATION OPERATORS"), R("537081", "TRN-REFUSE AND RECYCLABLE MATERIAL COLLECTORS"),
        R("5370XX", "TRN-CONVEYOR OPERATORS AND TENDERS, AND HOIST AND WINCH OPERATORS "),
        R("5371XX",
            "TRN-MISCELLANEOUS MATERIAL MOVING WORKERS, INCLUDING SHUTTLE CAR OPERATORS, AND TANK CAR, TRUCK, AND SHIP LOADERS"),
        R("551010", "MIL-MILITARY OFFICER SPECIAL AND TACTICAL OPERATIONS LEADERS/MANAGERS"),
        R("552010", "MIL-FIRST-LINE ENLISTED MILITARY SUPERVISORS/MANAGERS"),
        R("553010", "MIL-MILITARY ENLISTED TACTICAL OPERATIONS AND AIR/WEAPONS SPECIALISTS AND CREW MEMBERS"),
        R("559830", "MIL-MILITARY, RANK NOT SPECIFIED "),
        R("999920", "UNEMPLOYED AND LAST WORKED 5 YEARS AGO OR EARLIER OR NEVER WORKED"))));
    colInfo.put("SPORDER", new Triple<>("person_number", ColumnType.LONG, longFn()));
    colInfo.put("SRNT", new Triple<>("specified_rent_unit", ColumnType.LONG, bool()));
    colInfo.put("SSIP", new Triple<>("supplementary_security_income_12_months", ColumnType.LONG, longSpecial(//
        R("bbbbb", -1L))));
    colInfo.put("SSP", new Triple<>("social_security_income_12_months", ColumnType.LONG, longSpecial(//
        R("bbbbb", -1L))));
    colInfo.put("ST",
        new Triple<>("state", ColumnType.STRING, new ReplaceFn(//
            R("01", "Alabama/AL"), R("02", "Alaska/AK"), R("04", "Arizona/AZ"), R("05", "Arkansas/AR"),
            R("06", "California/CA"), R("08", "Colorado/CO"), R("09", "Connecticut/CT"), R("10", "Delaware/DE"),
            R("11", "District of Columbia/DC"), R("12", "Florida/FL"), R("13", "Georgia/GA"), R("15", "Hawaii/HI"),
            R("16", "Idaho/ID"), R("17", "Illinois/IL"), R("18", "Indiana/IN"), R("19", "Iowa/IA"),
            R("20", "Kansas/KS"), R("21", "Kentucky/KY"), R("22", "Louisiana/LA"), R("23", "Maine/ME"),
            R("24", "Maryland/MD"), R("25", "Massachusetts/MA"), R("26", "Michigan/MI"), R("27", "Minnesota/MN"),
            R("28", "Mississippi/MS"), R("29", "Missouri/MO"), R("30", "Montana/MT"), R("31", "Nebraska/NE"),
            R("32", "Nevada/NV"), R("33", "New Hampshire/NH"), R("34", "New Jersey/NJ"), R("35", "New Mexico/NM"),
            R("36", "New York/NY"), R("37", "North Carolina/NC"), R("38", "North Dakota/ND"), R("39", "Ohio/OH"),
            R("40", "Oklahoma/OK"), R("41", "Oregon/OR"), R("42", "Pennsylvania/PA"), R("44", "Rhode Island/RI"),
            R("45", "South Carolina/SC"), R("46", "South Dakota/SD"), R("47", "Tennessee/TN"), R("48", "Texas/TX"),
            R("49", "Utah/UT"), R("50", "Vermont/VT"), R("51", "Virginia/VA"), R("53", "Washington/WA"),
            R("54", "West Virginia/WV"), R("55", "Wisconsin/WI"), R("56", "Wyoming/WY"), R("72", "Puerto Rico/PR"))));
    colInfo.put("SVAL", new Triple<>("value_owner_unit", ColumnType.LONG, bool()));
    colInfo.put("TAXP",
        new Triple<>("property_taxes_yearly",
            // new ReplaceFn(//
            // R("bb", "N/A (GQ/vacant/not owned or being bought)"), R("01", "None"), R("02", "$ 1 - $ 49"),
            // R("03", "$ 50 - $ 99"), R("04", "$ 100 - $ 149"), R("05", "$ 150 - $ 199"), R("06", "$ 200 - $ 249"),
            // R("07", "$ 250 - $ 299"), R("08", "$ 300 - $ 349"), R("09", "$ 350 - $ 399"), R("10", "$ 400 - $ 449"),
            // R("11", "$ 450 - $ 499"), R("12", "$ 500 - $ 549"), R("13", "$ 550 - $ 599"), R("14", "$ 600 - $ 649"),
            // R("15", "$ 650 - $ 699"), R("16", "$ 700 - $ 749"), R("17", "$ 750 - $ 799"), R("18", "$ 800 - $ 849"),
            // R("19", "$ 850 - $ 899"), R("20", "$ 900 - $ 949"), R("21", "$ 950 - $ 999"), R("22", "$1000 - $1099"),
            // R("23", "$1100 - $1199"), R("24", "$1200 - $1299"), R("25", "$1300 - $1399"), R("26", "$1400 - $1499"),
            // R("27", "$1500 - $1599"), R("28", "$1600 - $1699"), R("29", "$1700 - $1799"), R("30", "$1800 - $1899"),
            // R("31", "$1900 - $1999"), R("32", "$2000 - $2099"), R("33", "$2100 - $2199"), R("34", "$2200 - $2299"),
            // R("35", "$2300 - $2399"), R("36", "$2400 - $2499"), R("37", "$2500 - $2599"), R("38", "$2600 - $2699"),
            // R("39", "$2700 - $2799"), R("40", "$2800 - $2899"), R("41", "$2900 - $2999"), R("42", "$3000 - $3099"),
            // R("43", "$3100 - $3199"), R("44", "$3200 - $3299"), R("45", "$3300 - $3399"), R("46", "$3400 - $3499"),
            // R("47", "$3500 - $3599"), R("48", "$3600 - $3699"), R("49", "$3700 - $3799"), R("50", "$3800 - $3899"),
            // R("51", "$3900 - $3999"), R("52", "$4000 - $4099"), R("53", "$4100 - $4199"), R("54", "$4200 - $4299"),
            // R("55", "$4300 - $4399"), R("56", "$4400 - $4499"), R("57", "$4500 - $4599"), R("58", "$4600 - $4699"),
            // R("59", "$4700 - $4799"), R("60", "$4800 - $4899"), R("61", "$4900 - $4999"), R("62", "$5000 - $5499"),
            // R("63", "$5500 - $5999"), R("64", "$6000 - $6999"), R("65", "$7000 - $7999"), R("66", "$8000 - $8999"),
            // R("67", "$9000 - $9999"), R("68", "$10000+"))));
            ColumnType.LONG,
            longSpecial(//
                R("bb", -1L), R("01", 0L), R("02", 1L), R("03", 50L), R("04", 100L), R("05", 150L), R("06", 200L),
                R("07", 250L), R("08", 300L), R("09", 350L), R("10", 400L), R("11", 450L), R("12", 500L), R("13", 550L),
                R("14", 600L), R("15", 650L), R("16", 700L), R("17", 750L), R("18", 800L), R("19", 850L), R("20", 900L),
                R("21", 950L), R("22", 1000L), R("23", 1100L), R("24", 1200L), R("25", 1300L), R("26", 1400L),
                R("27", 1500L), R("28", 1600L), R("29", 1700L), R("30", 1800L), R("31", 1900L), R("32", 2000L),
                R("33", 2100L), R("34", 2200L), R("35", 2300L), R("36", 2400L), R("37", 2500L), R("38", 2600L),
                R("39", 2700L), R("40", 2800L), R("41", 2900L), R("42", 3000L), R("43", 3100L), R("44", 3200L),
                R("45", 3300L), R("46", 3400L), R("47", 3500L), R("48", 3600L), R("49", 3700L), R("50", 3800L),
                R("51", 3900L), R("52", 4000L), R("53", 4100L), R("54", 4200L), R("55", 4300L), R("56", 4400L),
                R("57", 4500L), R("58", 4600L), R("59", 4700L), R("60", 4800L), R("61", 4900L), R("62", 5000L),
                R("63", 5500L), R("64", 6000L), R("65", 7000L), R("66", 8000L), R("67", 9000L), R("68", 10000L))));
    colInfo.put("TEL", new Triple<>("telephone_in_house", ColumnType.LONG, longSpecial(//
        R("b", -1L), R("1", 1L), R("2", 0L))));
    colInfo.put("TEN",
        new Triple<>("tenure", ColumnType.STRING,
            new ReplaceFn(//
                R("b", "n/a"), R("1", "Owned with mortgage or loan (include home equity loans)"),
                R("2", "Owned free and clear"), R("3", "Rented"), R("4", "Occupied without payment of rent"))));
    colInfo.put("TYPE",
        new Triple<>("type", ColumnType.STRING,
            new ReplaceFn(//
                R("1", "Housing unit"), R("2", "Institutional group quarters"),
                R("3", "Noninstitutional group quarters"))));
    colInfo.put("VACS", new Triple<>("vacancy_status", ColumnType.STRING,
        new ReplaceFn(R("b", "N/A (occupied/GQ)"), R("1", "For rent"), R("2", "Rented, not occupied"),
            R("3", "For sale only"), R("4", "Sold, not occupied"), R("5", "For seasonal/recreational/occasional use"),
            R("6", "For migrant workers"), R("7", "Other vacant"))));
    colInfo.put("VAL",
        new Triple<>("property_value",
            // new ReplaceFn(//
            // R("bb", "N/A (GQ/rental unit/vacant, except for-sale-only and sold, not occupied”)"),
            // R("01", "Less than $ 10000"), R("02", "$ 10000 - $ 14999"), R("03", "$ 15000 - $ 19999"),
            // R("04", "$ 20000 - $ 24999"), R("05", "$ 25000 - $ 29999"), R("06", "$ 30000 - $ 34999"),
            // R("07", "$ 35000 - $ 39999"), R("08", "$ 40000 - $ 49999"), R("09", "$ 50000 - $ 59999"),
            // R("10", "$ 60000 - $ 69999"), R("11", "$ 70000 - $ 79999"), R("12", "$ 80000 - $ 89999"),
            // R("13", "$ 90000 - $ 99999"), R("14", "$100000 - $124999"), R("15", "$125000 - $149999"),
            // R("16", "$150000 - $174999"), R("17", "$175000 - $199999"), R("18", "$200000 - $249999"),
            // R("19", "$250000 - $299999"), R("20", "$300000 - $399999"), R("21", "$400000 - $499999"),
            // R("22", "$500000 - $749999"), R("23", "$750000 - $999999"), R("24", "$1000000+"))));
            ColumnType.LONG,
            longSpecial(//
                R("bb", -1L), R("01", 0L), R("02", 10000L), R("03", 15000L), R("04", 20000L), R("05", 25000L),
                R("06", 30000L), R("07", 35000L), R("08", 40000L), R("09", 50000L), R("10", 60000L), R("11", 70000L),
                R("12", 80000L), R("13", 90000L), R("14", 100000L), R("15", 125000L), R("16", 150000L),
                R("17", 175000L), R("18", 200000L), R("19", 250000L), R("20", 300000L), R("21", 400000L),
                R("22", 500000L), R("23", 750000L), R("24", 1000000L))));

    colInfo.put("VEH", new Triple<>("vehicles", ColumnType.LONG, longSpecial(//
        R("b", -1L))));
    colInfo.put("VPS",
        new Triple<>("veteran_period_of_service", ColumnType.STRING, new ReplaceFn(//
            R("bb", "N/A (less than 17 years old, no active duty) "), R("01", "Gulf War: 9/2001 or later"),
            R("02", "Gulf War: 9/2001 or later and Gulf War: 8/1990 - 8/2001"),
            R("03", "Gulf War: 9/2001 or later and Gulf War: 8/1990 - 8/2001 and Vietnam Era"),
            R("04", "Gulf War: 8/1990 - 8/2001"), R("05", "Gulf War: 8/1990 - 8/2001 and Vietnam Era"),
            R("06", "Vietnam Era"), R("07", "Vietnam Era and Korean War"), R("08", "Vietnam Era, Korean War, and WWII"),
            R("09", "Korean War"), R("1", "Gulf War: 9/2001 or later"),
            R("2", "Gulf War: 9/2001 or later and Gulf War: 8/1990 - 8/2001"),
            R("3", "Gulf War: 9/2001 or later and Gulf War: 8/1990 - 8/2001 and Vietnam Era"),
            R("4", "Gulf War: 8/1990 - 8/2001"), R("5", "Gulf War: 8/1990 - 8/2001 and Vietnam Era"),
            R("6", "Vietnam Era"), R("7", "Vietnam Era and Korean War"), R("8", "Vietnam Era, Korean War, and WWII"),
            R("9", "Korean War"), R("10", "Korean War and WWII"), R("11", "WWII"),
            R("12", "Between Gulf War and Vietnam Era only"), R("13", "Between Vietnam Era and Korean War only"),
            R("14", "Between Korean War and World War II only"), R("15", "Pre-WWII only"))));
    colInfo.put("WAGP", new Triple<>("wages_salary_income_12_months", ColumnType.LONG, longSpecial(//
        R("bbbbbb", -1L))));
    colInfo.put("WAOB",
        new Triple<>("world_area_of_birth", ColumnType.STRING,
            new ReplaceFn(//
                R("1", "US state (POB = 001-059)"), R("2", "PR and US Island Areas (POB = 060-099)"),
                R("3", "Latin America (POB = 303,310-399)"), R("4", "Asia (POB = 158-159,161,200-299)"),
                R("5", "Europe (POB = 100-157,160,162-199)"), R("6", "Africa (POB = 400-499)"),
                R("7", "Northern America (POB = 300-302,304-309)"), R("8", "Oceania and at Sea (POB = 500-554)"))));
    colInfo.put("WATP", new Triple<>("water_cost_yearly", ColumnType.LONG, longSpecial(//
        R("bbbb", -1L))));
    colInfo.put("WGTP", new Triple<>("housing_weight", ColumnType.LONG, longFn()));
    colInfo.put("WGTPR", new Triple<>("housing_weight_replicate", ColumnType.LONG, longFn()));
    colInfo.put("WIF", new Triple<>("workers_in_family_12_months", ColumnType.LONG, longSpecial(//
        R("b", -1L))));
    colInfo.put("WKEXREL",
        new Triple<>("work_experience_of_householder", ColumnType.STRING,
            new ReplaceFn(//
                R("b", "N/A (GQ/vacant/not a family)"), R("1", "Householder and spouse worked FT"),
                R("2", "Householder worked FT; spouse worked < FT"),
                R("3", "Householder worked FT; spouse did not work"), R("4",
                    "Householder worked < FT; spouse worked FT"),
                R("5", "Householder worked < FT; spouse worked < FT"),
                R("6", "Householder worked < FT; spouse did not work"),
                R("7", "Householder did not work; spouse worked FT"),
                R("8", "Householder did not work; spouse worked < FT"),
                R("9", "Householder did not work; spouse did not work"),
                R("10", "Male householder worked FT; no spouse present"),
                R("11", "Male householder worked < FT; no spouse present"),
                R("12", "Male householder did not work; no spouse present"),
                R("13", "Female householder worked FT; no spouse present"),
                R("14", "Female householder worked < FT; no spouse present"),
                R("15", "Female householder did not work; no spouse present"))));
    colInfo.put("WKHP", new Triple<>("hours_worked_per_week", ColumnType.LONG, longSpecial(//
        R("bb", -1L))));
    colInfo.put("WKL",
        new Triple<>("last_worked", ColumnType.STRING,
            new ReplaceFn(//
                R("b", "N/A (less than 16 years old)"), R("1", "Within the past 12 months"), R("2", "1-5 years ago"),
                R("3", "Over 5 years ago or never worked"))));
    colInfo.put("WKW",
        new Triple<>("weeks_worked_12_months", ColumnType.STRING,
            new ReplaceFn(//
                R("b", "N/A (less than 16 years old/did not work during the past 12 months)"), R("1", "50 to 52 weeks"),
                R("2", "48 to 49 weeks"), R("3", "40 to 47 weeks"), R("4", "27 to 39 weeks"), R("5", "14 to 26 weeks"),
                R("6", "13 weeks or less"))));
    colInfo
        .put("WORKSTAT",
            new Triple<>("work_status_of_householder", ColumnType.STRING,
                new ReplaceFn(//
                    R("bb", "N/A (GQ/not a family household)"),
                    R("1", "Husband and wife both in labor force, both employed or in Armed Forces"),
                    R("2",
                        "Husband and wife both in labor force, husband employed or in Armed Forces, wife unemployed"),
            R("3", "Husband in labor force and wife not in labor force, husband employed or in Armed Forces"),
            R("4", "Husband and wife both in labor force, husband unemployed, wife employed or in Armed Forces"),
            R("5", "Husband and wife both in labor force, husband unemployed, wife unemployed"),
            R("6", "Husband in labor force, husband unemployed, wife not in labor force"),
            R("7", "Husband not in labor force, wife in labor force, wife employed or in Armed Forces"),
            R("8", "Husband not in labor force, wife in labor force, wife unemployed"),
            R("9", "Neither husband nor wife in labor force"),
            R("10", "Male householder with no wife present, householder in labor force, employed or in Armed Forces"),
            R("11", "Male householder with no wife present, householder in labor force and unemployed"),
            R("12", "Male householder with no wife present, householder not in labor force"),
            R("13",
                "Female householder with no husband present, householder in labor force, employed or in Armed Forces"),
        R("14", "Female householder with no husband present, householder in labor force and unemployed"),
        R("15", "Female householder with no husband present, householder not in labor force"))));
    colInfo.put("YBL",
        new Triple<>("year_building_built", ColumnType.STRING,
            new ReplaceFn(//
                R("b", "N/A (GQ)"), R("1", "2005 or later"), R("2", "2000 to 2004"), R("3", "1990 to 1999"),
                R("4", "1980 to 1989"), R("5", "1970 to 1979"), R("6", "1960 to 1969"), R("7", "1950 to 1959"),
                R("8", "1940 to 1949"), R("9", "1939 or earlier"))));
    colInfo.put("YOEP", new Triple<>("year_of_entry", ColumnType.LONG, longSpecial(//
        R("bbbb", -1))));
  }
}
