import unittest
from pathlib import Path
R = Path(__file__).resolve().parents[1]
JAVA = R / 'app/src/main/java/fr/friday/assistant'
class GpsMistralTest(unittest.TestCase):
    def test_removed_google_places(self):
        self.assertFalse((JAVA/'GooglePlacesNearby.java').exists())
        for f in ('MainActivity.java','FridayService.java','NativeAI.java'):
            txt=(JAVA/f).read_text()
            self.assertNotIn('GooglePlacesNearby',txt)
            self.assertNotIn('places_enabled',txt)
    def test_gps_target_is_not_fake(self):
        txt=(JAVA/'FridayLocation.java').read_text()
        self.assertIn('TARGET_METERS = 3f',txt)
        self.assertIn('accuracy <= TARGET_METERS',txt)
        self.assertIn('LocationManager.GPS_PROVIDER.equals(provider)',txt)
        self.assertIn('Objectif 3 mètres non atteint',txt)
        self.assertIn('lm.removeUpdates(listener)',txt)
    def test_web_search_required_for_places(self):
        service=(JAVA/'FridayService.java').read_text()
        ai=(JAVA/'NativeAI.java').read_text()
        self.assertIn('RECHERCHE_ALENTOURS_WEB',service)
        self.assertIn('surroundings',ai)
        self.assertIn('web_search',ai)
        self.assertIn('location_share',service)
    def test_no_google_ui(self):
        html=(R/'app/src/main/assets/index.html').read_text()
        self.assertNotIn('Google Maps',html)
        self.assertNotIn('Google Places',html)
        self.assertIn('gpsBtn',html)
        self.assertIn('nearbyBtn',html)
if __name__ == '__main__': unittest.main()
