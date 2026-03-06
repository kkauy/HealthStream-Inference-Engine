#include <iostream>
#include <string>
#include "json.hpp"

using json = nlohmann::json;

int main() {

    std::string input;

    while (std::getline(std::cin, input)) {

        try {

            json req = json::parse(input);

            if (!req.contains("id") || !req.contains("features")) {

                json error = {
                        {"ok", false},
                        {"error", "invalid request format"}
                };

                std::cout << error.dump() << std::endl;
                continue;
            }

            auto features = req["features"];

            if (features.size() != 30) {

                json error = {
                        {"ok", false},
                        {"error", "invalid feature vector length"}
                };

                std::cout << error.dump() << std::endl;
                continue;
            }

            json ok = {
                    {"ok", true},
                    {"message", "relay validation passed"}
            };

            std::cout << ok.dump() << std::endl;

        } catch (...) {

            json error = {
                    {"ok", false},
                    {"error", "json parse error"}
            };

            std::cout << error.dump() << std::endl;
        }
    }

    return 0;
}