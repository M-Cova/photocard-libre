import unittest
from pathlib import Path
from xml.etree import ElementTree


RESOURCES = Path(__file__).parents[1] / "app/src/main/res"


class SettingsResourceTests(unittest.TestCase):
    def test_measurement_unit_strings_exist_in_every_supported_language(self):
        expected = {
            "values-it": ("UNITÀ DI MISURA", "Centimetri", "Pollici"),
            "values-en": ("MEASUREMENT UNIT", "Centimeters", "Inches"),
            "values-es": ("UNIDAD DE MEDIDA", "Centímetros", "Pulgadas"),
            "values-de": ("MASSEINHEIT", "Zentimeter", "Zoll"),
            "values-fr": ("UNITÉ DE MESURE", "Centimètres", "Pouces"),
            "values-pt": ("UNIDADE DE MEDIDA", "Centímetros", "Polegadas"),
        }
        keys = (
            "measurement_unit",
            "measurement_unit_centimeters",
            "measurement_unit_inches",
        )

        for directory, translations in expected.items():
            with self.subTest(directory=directory):
                root = ElementTree.parse(RESOURCES / directory / "strings.xml").getroot()
                strings = {item.attrib["name"]: item.text for item in root.findall("string")}
                self.assertEqual(dict(zip(keys, translations)), {key: strings[key] for key in keys})


if __name__ == "__main__":
    unittest.main()
