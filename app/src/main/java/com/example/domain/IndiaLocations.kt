package com.example.domain

data class IndianState(
    val name: String,
    val code: String,
    val districts: List<String>
)

object IndiaLocations {
    val states: List<IndianState> = listOf(
        IndianState("Andhra Pradesh", "AP", listOf("Visakhapatnam", "Vijayawada", "Guntur", "Tirupati", "Kakinada", "Nellore", "Kurnool", "Rajahmundry")),
        IndianState("Arunachal Pradesh", "AR", listOf("Itanagar", "Tawang", "Naharlagun", "Pasighat", "Ziro")),
        IndianState("Assam", "AS", listOf("Guwahati", "Silchar", "Dibrugarh", "Jorhat", "Nagaon", "Tezpur")),
        IndianState("Bihar", "BR", listOf("Patna", "Gaya", "Muzaffarpur", "Bhagalpur", "Darbhanga", "Purnia", "Begusarai")),
        IndianState("Chhattisgarh", "CG", listOf("Raipur", "Bilaspur", "Durg", "Bhilai", "Korba", "Rajnandgaon")),
        IndianState("Goa", "GA", listOf("North Goa", "South Goa", "Panaji", "Margao", "Vasco da Gama")),
        IndianState("Gujarat", "GJ", listOf("Ahmedabad", "Surat", "Vadodara", "Rajkot", "Bhavnagar", "Jamnagar", "Gandhinagar", "Bharuch")),
        IndianState("Haryana", "HR", listOf("Gurugram", "Faridabad", "Panipat", "Ambala", "Hisar", "Rohtak", "Karnal", "Sonipat")),
        IndianState("Himachal Pradesh", "HP", listOf("Shimla", "Kangra", "Mandi", "Solan", "Kullu", "Una")),
        IndianState("Jharkhand", "JH", listOf("Ranchi", "Jamshedpur", "Dhanbad", "Bokaro", "Hazaribagh", "Deoghar")),
        IndianState("Karnataka", "KA", listOf("Bengaluru Urban", "Mysuru", "Hubballi-Dharwad", "Mangaluru", "Belagavi", "Kalaburagi", "Tumakuru")),
        IndianState("Kerala", "KL", listOf("Thiruvananthapuram", "Kochi", "Kozhikode", "Thrissur", "Kollam", "Kannur")),
        IndianState("Madhya Pradesh", "MP", listOf("Bhopal", "Indore", "Jabalpur", "Gwalior", "Ujjain", "Sagar", "Rewa")),
        IndianState("Maharashtra", "MH", listOf("Mumbai", "Pune", "Nagpur", "Thane", "Nashik", "Chhatrapati Sambhajinagar", "Solapur", "Kolhapur")),
        IndianState("Manipur", "MN", listOf("Imphal West", "Imphal East", "Thoubal", "Bishnupur", "Churachandpur")),
        IndianState("Meghalaya", "ML", listOf("East Khasi Hills", "Shillong", "Tura", "Jowai", "Ri-Bhoi")),
        IndianState("Mizoram", "MZ", listOf("Aizawl", "Lunglei", "Champhai", "Serchhip", "Kolasib")),
        IndianState("Nagaland", "NL", listOf("Kohima", "Dimapur", "Mokokchung", "Tuensang", "Wokha")),
        IndianState("Odisha", "OD", listOf("Bhubaneswar", "Cuttack", "Rourkela", "Sambalpur", "Berhampur", "Puri", "Balasore")),
        IndianState("Punjab", "PB", listOf("Ludhiana", "Amritsar", "Jalandhar", "Patiala", "Bathinda", "Mohali")),
        IndianState("Rajasthan", "RJ", listOf("Jaipur", "Jodhpur", "Udaipur", "Kota", "Bikaner", "Ajmer", "Alwar", "Bhilwara")),
        IndianState("Sikkim", "SK", listOf("Gangtok", "Namchi", "Gyalshing", "Mangan")),
        IndianState("Tamil Nadu", "TN", listOf("Chennai", "Coimbatore", "Madurai", "Tiruchirappalli", "Salem", "Tirunelveli", "Erode", "Vellore")),
        IndianState("Telangana", "TS", listOf("Hyderabad", "Warangal", "Nizamabad", "Karimnagar", "Khammam", "Rangareddy", "Medchal")),
        IndianState("Tripura", "TR", listOf("Agartala", "Udaipur", "Dharmanagar", "Kailashahar")),
        IndianState("Uttar Pradesh", "UP", listOf("Lucknow", "Noida", "Kanpur", "Varanasi", "Prayagraj", "Agra", "Ghaziabad", "Meerut", "Gorakhpur")),
        IndianState("Uttarakhand", "UK", listOf("Dehradun", "Haridwar", "Haldwani", "Roorkee", "Rudrapur", "Rishikesh")),
        IndianState("West Bengal", "WB", listOf("Kolkata", "Howrah", "Siliguri", "Durgapur", "Asansol", "Bardhaman", "Kharagpur")),
        IndianState("Andaman & Nicobar Islands", "AN", listOf("Port Blair", "Nicobar", "North and Middle Andaman")),
        IndianState("Chandigarh", "CH", listOf("Chandigarh")),
        IndianState("Dadra & Nagar Haveli and Daman & Diu", "DN", listOf("Daman", "Diu", "Silvassa")),
        IndianState("Delhi (NCT)", "DL", listOf("New Delhi", "Central Delhi", "North Delhi", "South Delhi", "East Delhi", "West Delhi", "Dwarka")),
        IndianState("Jammu & Kashmir", "JK", listOf("Srinagar", "Jammu", "Anantnag", "Baramulla", "Udhampur")),
        IndianState("Ladakh", "LA", listOf("Leh", "Kargil")),
        IndianState("Lakshadweep", "LD", listOf("Kavaratti", "Agatti", "Minicoy")),
        IndianState("Puducherry", "PY", listOf("Puducherry", "Karaikal", "Mahe", "Yanam"))
    )

    val stateNames: List<String> = states.map { it.name }

    fun districtsForState(stateName: String): List<String> {
        return states.firstOrNull { it.name.equals(stateName, ignoreCase = true) }?.districts
            ?: listOf("District HQ")
    }

    fun codeForState(stateName: String): String {
        return states.firstOrNull { it.name.equals(stateName, ignoreCase = true) }?.code ?: "IN"
    }
}
