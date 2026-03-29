import React from 'react'
import { createBottomTabNavigator } from '@react-navigation/bottom-tabs'
import { createNativeStackNavigator } from '@react-navigation/native-stack'
import HomeScreen from '@/screens/HomeScreen'
import PredictScreen from '@/screens/PredictScreen'
import LeaguesScreen from '@/screens/LeaguesScreen'
import LeagueDetailScreen from '@/screens/LeagueDetailScreen'
import ProfileScreen from '@/screens/ProfileScreen'
import { colors } from '@/theme/tokens'

export type AppTabParamList = {
  Home: undefined
  Predict: undefined
  LeaguesTab: undefined
  Profile: undefined
}

export type LeagueStackParamList = {
  Leagues: undefined
  LeagueDetail: { leagueId: string; leagueName: string }
}

const Tab = createBottomTabNavigator<AppTabParamList>()
const LeagueStack = createNativeStackNavigator<LeagueStackParamList>()

function LeaguesStackNavigator() {
  return (
    <LeagueStack.Navigator screenOptions={{ headerShown: false }}>
      <LeagueStack.Screen name="Leagues" component={LeaguesScreen} />
      <LeagueStack.Screen name="LeagueDetail" component={LeagueDetailScreen} />
    </LeagueStack.Navigator>
  )
}

export default function AppNavigator() {
  return (
    <Tab.Navigator
      screenOptions={{
        headerShown: false,
        tabBarStyle: { backgroundColor: colors.surface, borderTopColor: colors.border },
        tabBarActiveTintColor: colors.primary,
        tabBarInactiveTintColor: colors.textMuted,
      }}
    >
      <Tab.Screen name="Home" component={HomeScreen} options={{ tabBarLabel: 'Home' }} />
      <Tab.Screen name="Predict" component={PredictScreen} options={{ tabBarLabel: 'Predict' }} />
      <Tab.Screen name="LeaguesTab" component={LeaguesStackNavigator} options={{ tabBarLabel: 'Leagues' }} />
      <Tab.Screen name="Profile" component={ProfileScreen} options={{ tabBarLabel: 'Profile' }} />
    </Tab.Navigator>
  )
}
