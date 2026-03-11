#include <iostream>
#include <string>
#include <cctype>
#include "json.hpp"

using json = nlohmann::json;

// Helper function to trim whitespace from a string
std::string trim(const std::string& s) {
    size_t start = 0;
    while (start < s.size() && std::isspace(static_cast<unsigned char>(s[start]))) {
        start++;
    }

    size_t end = s.size();
    while (end > start && std::isspace(static_cast<unsigned char>(s[end - 1]))) {
        end--;
    }

    return s.substr(start, end - start);
}


int main() {
    std::string input;

    while (std::getline(std::cin, input)) {
        try {
            json req = json::parse(input);

            std::string requestId = "unknown";
            if (req.contains("id") && req["id"].is_string()) {
                requestId = req["id"];
            }

            // 1. Required field checks
            if (!req.contains("id")) {
                json error = {
                    {"ok", false},
                    {"id", requestId},
                    {"error", "missing required field: id"}
                };

                std::cout << error.dump() << std::endl;
                continue;
            }

            if (!req.contains("task")) {
                json error = {
                    {"ok", false},
                    {"id", requestId},
                    {"error", "missing required field: task"}
                };

                std::cout << error.dump() << std::endl;
                continue;
            }

            if (!req.contains("features")) {
                json error = {
                    {"ok", false},
                    {"id", requestId},
                    {"error", "missing required field: features"}
                };

                std::cout << error.dump() << std::endl;
                continue;
            }

            // 2. Type checks
            if (!req["id"].is_string()) {
                json error = {
                    {"ok", false},
                    {"id", requestId},
                    {"error", "invalid request format: id must be a string"}
                };

                std::cout << error.dump() << std::endl;
                continue;
            }

            if (!req["task"].is_string()) {
                json error = {
                    {"ok", false},
                    {"id", requestId},
                    {"error", "invalid request format: task must be a string"}
                };

                std::cout << error.dump() << std::endl;
                continue;
            }

            if (!req["features"].is_array()) {
                json error = {
                    {"ok", false},
                    {"id", requestId},
                    {"error", "invalid request format: features must be an array"}
                };

                std::cout << error.dump() << std::endl;
                continue;
            }

            // 3. Task validation
            std::string task = trim(req["task"].get<std::string>());

            if (task.empty()) {
                json error = {
                    {"ok", false},
                    {"id", requestId},
                    {"error", "invalid request format: task cannot be empty"}
                };

                std::cout << error.dump() << std::endl;
                continue;
            }

            if (task != "breast_cancer") {
                json error = {
                    {"ok", false},
                    {"id", requestId},
                    {"error", "unsupported task"}
                };

                std::cout << error.dump() << std::endl;
                continue;
            }

            // 4. Feature length validation
            auto features = req["features"];
            if (features.size() != 30) {
                json error = {
                    {"ok", false},
                    {"id", requestId},
                    {"error", "invalid feature vector length"}
                };

                std::cout << error.dump() << std::endl;
                continue;
            }

            // 5. Feature numeric validation
            bool allNumeric = true;
            for (const auto& feature : features) {
                if (!feature.is_number()) {
                    allNumeric = false;
                    break;
                }
            }

            if (!allNumeric) {
                json error = {
                    {"ok", false},
                    {"id", requestId},
                    {"error", "all features must be numeric"}
                };

                std::cout << error.dump() << std::endl;
                continue;
            }

            // 6. Success response
            json ok = {
                {"ok", true},
                {"id", requestId},
                {"message", "relay validation passed"}
            };

            std::cout << ok.dump() << std::endl;

        } catch (const json::parse_error&) {
            json error = {
                {"ok", false},
                {"id", "unknown"},
                {"error", "json parse error"}
            };

            std::cout << error.dump() << std::endl;

        } catch (const std::exception& e) {
            json error = {
                {"ok", false},
                {"id", "unknown"},
                {"error", std::string("relay exception: ") + e.what()}
            };

            std::cout << error.dump() << std::endl;

        } catch (...) {
            json error = {
                {"ok", false},
                {"id", "unknown"},
                {"error", "unknown relay error"}
            };

            std::cout << error.dump() << std::endl;
        }
    }

    return 0;
}